package io.jact.core;

import io.jact.annotations.JNode;
import io.jact.core.api.Hooks;
import io.jact.core.api.State;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.meta.ComponentDescriptor;
import io.jact.core.meta.PageDescriptor;
import io.jact.core.node.TextNode;
import io.jact.core.node.Nodes;
import io.jact.core.registry.RuntimeRegistry;
import io.jact.core.runtime.JactRuntime;
import io.jact.core.runtime.RenderRequest;
import io.jact.core.runtime.WindowSettings;
import io.jact.core.runtime.spi.RendererBridge;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JactRuntimeTest {
    @Test
    void mountsResolvedPageIntoRenderer() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of();
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.SamplePages", "home"));
            }
        };

        AtomicBoolean started = new AtomicBoolean();
        AtomicReference<JNode> mountedNode = new AtomicReference<>();
        RendererBridge rendererBridge = new RendererBridge() {
            @Override
            public void ensureStarted() {
                started.set(true);
            }

            @Override
            public void mount(JNode rootNode, WindowSettings windowSettings) {
                mountedNode.set(rootNode);
            }

            @Override
            public void update(JNode rootNode) {
                mountedNode.set(rootNode);
            }

            @Override
            public void executeOnUiThread(Runnable task) {
                task.run();
            }
        };

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage("/", request -> Nodes.text("ok"), new WindowSettings("demo", 800, 600));

        assertThat(started.get()).isTrue();
        assertThat(mountedNode.get()).isNotNull();
    }

    @Test
    void extractsRouteParamsForDynamicRoute() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of();
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(
                    new PageDescriptor("/", "io.jact.sample.HomePages", "home"),
                    new PageDescriptor("/tasks/$id", "io.jact.sample.TaskPages", "task")
                );
            }
        };

        RendererBridge rendererBridge = new RendererBridge() {
            @Override
            public void ensureStarted() {
            }

            @Override
            public void mount(JNode rootNode, WindowSettings windowSettings) {
            }

            @Override
            public void update(JNode rootNode) {
            }

            @Override
            public void executeOnUiThread(Runnable task) {
                task.run();
            }
        };

        CopyOnWriteArrayList<RenderRequest> renderRequests = new CopyOnWriteArrayList<>();
        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage("/", request -> {
            renderRequests.add(request);
            return new TextNode("ok");
        }, new WindowSettings("demo", 800, 600));

        runtime.navigator().push("/tasks/42");

        RenderRequest lastRequest = renderRequests.get(renderRequests.size() - 1);
        assertThat(lastRequest.path()).isEqualTo("/tasks/42");
        assertThat(lastRequest.params().get("id")).isEqualTo("42");
        assertThat(runtime.navigator().currentParams().get("id")).isEqualTo("42");
    }

    @Test
    void appliesStateUpdateScheduledFromEffectInSameRenderCycle() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of();
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.HomePages", "home"));
            }
        };

        CopyOnWriteArrayList<String> renderedValues = new CopyOnWriteArrayList<>();
        RendererBridge rendererBridge = new RendererBridge() {
            @Override
            public void ensureStarted() {
            }

            @Override
            public void mount(JNode rootNode, WindowSettings windowSettings) {
                renderedValues.add(((TextNode) rootNode).value());
            }

            @Override
            public void update(JNode rootNode) {
                renderedValues.add(((TextNode) rootNode).value());
            }

            @Override
            public void executeOnUiThread(Runnable task) {
                task.run();
            }
        };

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage("/", request -> {
            State<Integer> value = Hooks.useState(0);
            Hooks.useEffect(() -> {
                if (value.get() == 0) {
                    value.set(1);
                }
            }, value.get());
            return Nodes.text("value=" + value.get());
        }, new WindowSettings("demo", 800, 600));

        assertThat(renderedValues).containsExactly("value=0", "value=1");
    }

    @Test
    void clearsHookContextWhenRenderFails() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of();
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.HomePages", "home"));
            }
        };

        RendererBridge rendererBridge = new RendererBridge() {
            @Override
            public void ensureStarted() {
            }

            @Override
            public void mount(JNode rootNode, WindowSettings windowSettings) {
            }

            @Override
            public void update(JNode rootNode) {
            }

            @Override
            public void executeOnUiThread(Runnable task) {
                task.run();
            }
        };

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();

        assertThatThrownBy(() -> runtime.mountInitialPage("/", request -> {
            throw new IllegalStateException("boom");
        }, new WindowSettings("demo", 800, 600))).isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> Hooks.useState(0))
            .isInstanceOf(JactRuntimeException.class)
            .hasMessageContaining("Hooks can only be called during a JACT render cycle.");
    }
}
