package io.jact.core;

import io.jact.annotations.JNode;
import io.jact.core.api.Hooks;
import io.jact.core.api.State;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.meta.ComponentDescriptor;
import io.jact.core.meta.PageDescriptor;
import io.jact.core.node.ContainerNode;
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
    void resolvesComponentNodesBeforeRendering() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of(new ComponentDescriptor("io.jact.sample.Components", "title"));
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.HomePage", "home"));
            }
        };

        AtomicReference<JNode> renderedNode = new AtomicReference<>();
        RendererBridge rendererBridge = new RendererBridge() {
            @Override
            public void ensureStarted() {
            }

            @Override
            public void mount(JNode rootNode, WindowSettings windowSettings) {
                renderedNode.set(rootNode);
            }

            @Override
            public void update(JNode rootNode) {
                renderedNode.set(rootNode);
            }

            @Override
            public void executeOnUiThread(Runnable task) {
                task.run();
            }
        };

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage(
            "/",
            request -> Nodes.component("title", "Dashboard"),
            request -> Nodes.text(request.arguments().getFirst().toString()),
            new WindowSettings("demo", 800, 600)
        );

        assertThat(renderedNode.get()).isEqualTo(new TextNode("Dashboard"));
    }

    @Test
    void failsWhenComponentCannotBeResolved() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of();
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.HomePage", "home"));
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

        assertThatThrownBy(() -> runtime.mountInitialPage(
            "/",
            request -> Nodes.component("missing"),
            request -> Nodes.text("unused"),
            new WindowSettings("demo", 800, 600)
        ))
            .isInstanceOf(JactRuntimeException.class)
            .hasMessageContaining("No component found for id: missing");
    }

    @Test
    void isolatesHookStatePerComponentInstance() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of(new ComponentDescriptor("io.jact.sample.CounterComponents", "counter"));
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.HomePage", "home"));
            }
        };

        CopyOnWriteArrayList<List<String>> renders = new CopyOnWriteArrayList<>();
        RendererBridge rendererBridge = new RendererBridge() {
            @Override
            public void ensureStarted() {
            }

            @Override
            public void mount(JNode rootNode, WindowSettings windowSettings) {
                renders.add(textValues(rootNode));
            }

            @Override
            public void update(JNode rootNode) {
                renders.add(textValues(rootNode));
            }

            @Override
            public void executeOnUiThread(Runnable task) {
                task.run();
            }
        };

        AtomicReference<State<Integer>> firstCounter = new AtomicReference<>();
        AtomicReference<State<Integer>> secondCounter = new AtomicReference<>();

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage(
            "/",
            request -> Nodes.column(
                Nodes.component("counter", "first"),
                Nodes.component("counter", "second")
            ),
            request -> {
                State<Integer> count = Hooks.useState(0);
                String name = request.arguments().getFirst().toString();
                if ("first".equals(name)) {
                    firstCounter.set(count);
                } else {
                    secondCounter.set(count);
                }
                return Nodes.text(name + "=" + count.get());
            },
            new WindowSettings("demo", 800, 600)
        );

        firstCounter.get().set(1);

        assertThat(secondCounter.get().get()).isZero();
        assertThat(renders).containsExactly(
            List.of("first=0", "second=0"),
            List.of("first=1", "second=0")
        );
    }

    @Test
    void cleansUpComponentHooksWhenComponentIsRemoved() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of(new ComponentDescriptor("io.jact.sample.Components", "tracked"));
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.HomePage", "home"));
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

        AtomicReference<State<Boolean>> visible = new AtomicReference<>();
        AtomicBoolean cleanedUp = new AtomicBoolean();

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage(
            "/",
            request -> {
                State<Boolean> showComponent = Hooks.useState(true);
                visible.set(showComponent);
                return showComponent.get()
                    ? Nodes.column(Nodes.component("tracked"))
                    : Nodes.column(Nodes.text("empty"));
            },
            request -> {
                Hooks.useEffect(() -> () -> cleanedUp.set(true));
                return Nodes.text("tracked");
            },
            new WindowSettings("demo", 800, 600)
        );

        visible.get().set(false);

        assertThat(cleanedUp.get()).isTrue();
    }

    @Test
    void shutdownRunsEffectCleanupAndDeactivatesState() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of();
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.HomePage", "home"));
            }
        };

        RendererBridge rendererBridge = noOpRenderer();
        AtomicBoolean cleanedUp = new AtomicBoolean();
        AtomicReference<State<Integer>> state = new AtomicReference<>();

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage("/", request -> {
            State<Integer> value = Hooks.useState(0);
            state.set(value);
            Hooks.useEffect(() -> () -> cleanedUp.set(true));
            return Nodes.text("value=" + value.get());
        }, new WindowSettings("demo", 800, 600));

        runtime.shutdown();
        state.get().set(1);

        assertThat(cleanedUp.get()).isTrue();
        assertThat(state.get().get()).isZero();
    }

    @Test
    void wrapsEffectFailures() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<ComponentDescriptor> components() {
                return List.of();
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.HomePage", "home"));
            }
        };

        JactRuntime runtime = new JactRuntime(runtimeRegistry, noOpRenderer());
        runtime.start();

        assertThatThrownBy(() -> runtime.mountInitialPage("/", request -> {
            Hooks.useEffect(() -> {
                throw new IllegalStateException("boom");
            });
            return Nodes.text("ok");
        }, new WindowSettings("demo", 800, 600)))
            .isInstanceOf(JactRuntimeException.class)
            .hasMessageContaining("JACT effect failed");
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

    private List<String> textValues(JNode node) {
        if (node instanceof TextNode textNode) {
            return List.of(textNode.value());
        }
        if (node instanceof ContainerNode containerNode) {
            return containerNode.children().stream()
                .flatMap(child -> textValues(child).stream())
                .toList();
        }
        return List.of();
    }

    private RendererBridge noOpRenderer() {
        return new RendererBridge() {
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
    }
}
