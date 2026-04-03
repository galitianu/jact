package io.jact.core;

import io.jact.annotations.JNode;
import io.jact.core.api.Hooks;
import io.jact.core.api.ObservableValue;
import io.jact.core.api.SimpleStore;
import io.jact.core.api.Store;
import io.jact.core.api.Subscription;
import io.jact.core.meta.ComponentDescriptor;
import io.jact.core.meta.PageDescriptor;
import io.jact.core.node.TextNode;
import io.jact.core.node.Nodes;
import io.jact.core.registry.RuntimeRegistry;
import io.jact.core.runtime.JactRuntime;
import io.jact.core.runtime.WindowSettings;
import io.jact.core.runtime.spi.RendererBridge;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HookStoreRuntimeTest {
    @Test
    void useStoreSelectorSkipsRerenderWhenSelectedValueIsUnchanged() {
        RuntimeRegistry runtimeRegistry = registryFor("/", "/other");
        List<String> renders = new CopyOnWriteArrayList<>();
        RendererBridge rendererBridge = recordingRenderer(renders);

        Store<StorePayload> store = new SimpleStore<>(new StorePayload("alpha", 0));

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage("/", request -> {
            String selected = Hooks.useStore(store, StorePayload::name);
            return Nodes.text("selected=" + selected);
        }, new WindowSettings("demo", 800, 600));

        store.set(new StorePayload("alpha", 1));
        store.set(new StorePayload("beta", 2));

        assertThat(renders).containsExactly("selected=alpha", "selected=beta");
    }

    @Test
    void useExternalUnsubscribesOnUnmount() {
        RuntimeRegistry runtimeRegistry = registryFor("/", "/other");
        List<String> renders = new CopyOnWriteArrayList<>();
        RendererBridge rendererBridge = recordingRenderer(renders);
        TestObservable observable = new TestObservable(0);

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage("/", request -> {
            if ("/other".equals(request.path())) {
                return Nodes.text("other");
            }
            Integer value = Hooks.useExternal(observable);
            return Nodes.text("value=" + value);
        }, new WindowSettings("demo", 800, 600));

        observable.set(1);
        runtime.navigator().push("/other");
        observable.set(2);

        assertThat(renders).containsExactly("value=0", "value=1", "other");
    }

    private RuntimeRegistry registryFor(String rootPath, String otherPath) {
        return new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of();
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(
                    new PageDescriptor(rootPath, "io.jact.sample.HomePages", "home"),
                    new PageDescriptor(otherPath, "io.jact.sample.OtherPages", "other")
                );
            }
        };
    }

    private RendererBridge recordingRenderer(List<String> renders) {
        return new RendererBridge() {
            @Override
            public void ensureStarted() {
            }

            @Override
            public void mount(JNode rootNode, WindowSettings windowSettings) {
                renders.add(((TextNode) rootNode).value());
            }

            @Override
            public void update(JNode rootNode) {
                renders.add(((TextNode) rootNode).value());
            }

            @Override
            public void executeOnUiThread(Runnable task) {
                task.run();
            }
        };
    }

    private record StorePayload(String name, int counter) {
    }

    private static final class TestObservable implements ObservableValue<Integer> {
        private final AtomicReference<Integer> value;
        private final List<Runnable> listeners = new ArrayList<>();

        private TestObservable(Integer initial) {
            this.value = new AtomicReference<>(initial);
        }

        @Override
        public Integer get() {
            return value.get();
        }

        @Override
        public Subscription subscribe(Runnable listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        private void set(Integer nextValue) {
            Integer previous = value.getAndSet(nextValue);
            if (previous.equals(nextValue)) {
                return;
            }
            for (Runnable listener : List.copyOf(listeners)) {
                listener.run();
            }
        }
    }
}
