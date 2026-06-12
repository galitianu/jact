package io.jact.spring.boot;

import io.jact.annotations.JNode;
import io.jact.core.api.Navigator;
import io.jact.core.meta.ComponentDescriptor;
import io.jact.core.meta.PageDescriptor;
import io.jact.core.node.Nodes;
import io.jact.core.node.TextNode;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.registry.RuntimeRegistry;
import io.jact.core.runtime.JactRuntime;
import io.jact.core.runtime.WindowSettings;
import io.jact.core.runtime.spi.RendererBridge;
import io.jact.spring.boot.autoconfigure.JactAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JactAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JactAutoConfiguration.class));

    @Test
    void registersCoreBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RuntimeRegistry.class);
            assertThat(context).doesNotHaveBean(JactRuntime.class);
            assertThat(context).doesNotHaveBean(RendererBridge.class);
            assertThat(context).doesNotHaveBean(ApplicationRunner.class);
        });
    }

    @Test
    void createsStartupRunnerWhenEnabled() {
        contextRunner
            .withPropertyValues("jact.enabled=true")
            .withBean(RendererBridge.class, () -> new RendererBridge() {
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
        })
        .run(context -> {
            assertThat(context).hasSingleBean(RuntimeRegistry.class);
            assertThat(context).hasSingleBean(JactRuntime.class);
            assertThat(context).hasSingleBean(Navigator.class);
            assertThat(context).hasSingleBean(ApplicationRunner.class);
        });
    }

    @Test
    void resolvesUnknownPageMethodArgumentsFromSpringContext() {
        AtomicReference<JNode> mountedNode = new AtomicReference<>();

        contextRunner
            .withPropertyValues("jact.enabled=true")
            .withUserConfiguration(PageInvocationTestConfig.class)
            .withBean(RendererBridge.class, () -> new RendererBridge() {
                @Override
                public void ensureStarted() {
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
            })
            .withBean(RuntimeRegistry.class, () -> new RuntimeRegistry() {
                @Override
                public List<ComponentDescriptor> components() {
                    return List.of();
                }

                @Override
                public List<PageDescriptor> pages() {
                    return List.of(new PageDescriptor("/", SamplePageBean.class.getName(), "home"));
                }
            })
            .run(context -> {
                ApplicationRunner runner = context.getBean(ApplicationRunner.class);
                try {
                    runner.run(null);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }

                assertThat(mountedNode.get()).isInstanceOf(TextNode.class);
                assertThat(((TextNode) mountedNode.get()).value()).isEqualTo("Resolved by Spring");
            });
    }

    @Test
    void failsClearlyWhenEnabledWithoutPages() {
        contextRunner
            .withPropertyValues("jact.enabled=true")
            .withBean(RendererBridge.class, JactAutoConfigurationTest::noOpRenderer)
            .withBean(RuntimeRegistry.class, () -> new RuntimeRegistry() {
                @Override
                public List<ComponentDescriptor> components() {
                    return List.of();
                }

                @Override
                public List<PageDescriptor> pages() {
                    return List.of();
                }
            })
            .run(context -> {
                ApplicationRunner runner = context.getBean(ApplicationRunner.class);
                assertThatThrownBy(() -> runner.run(null))
                    .isInstanceOf(JactRuntimeException.class)
                    .hasMessageContaining("no @JactPage entries were discovered");
            });
    }

    @Test
    void invokesComponentsThroughSpringBeans() {
        AtomicReference<JNode> mountedNode = new AtomicReference<>();

        contextRunner
            .withPropertyValues("jact.enabled=true")
            .withUserConfiguration(ComponentInvocationTestConfig.class)
            .withBean(RendererBridge.class, () -> recordingRenderer(mountedNode))
            .withBean(RuntimeRegistry.class, () -> new RuntimeRegistry() {
                @Override
                public List<ComponentDescriptor> components() {
                    return List.of(new ComponentDescriptor(ComponentBean.class.getName(), "header"));
                }

                @Override
                public List<PageDescriptor> pages() {
                    return List.of(new PageDescriptor("/", ComponentPageBean.class.getName(), "home"));
                }
            })
            .run(context -> {
                ApplicationRunner runner = context.getBean(ApplicationRunner.class);
                try {
                    runner.run(null);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }

                assertThat(mountedNode.get()).isEqualTo(new TextNode("Hello Component"));
            });
    }

    @Test
    void failsOnAmbiguousJactMethodOverloads() {
        contextRunner
            .withPropertyValues("jact.enabled=true")
            .withUserConfiguration(AmbiguousPageConfig.class)
            .withBean(RendererBridge.class, JactAutoConfigurationTest::noOpRenderer)
            .withBean(RuntimeRegistry.class, () -> new RuntimeRegistry() {
                @Override
                public List<ComponentDescriptor> components() {
                    return List.of();
                }

                @Override
                public List<PageDescriptor> pages() {
                    return List.of(new PageDescriptor("/", AmbiguousPageBean.class.getName(), "home"));
                }
            })
            .run(context -> {
                ApplicationRunner runner = context.getBean(ApplicationRunner.class);
                assertThatThrownBy(() -> runner.run(null))
                    .isInstanceOf(JactRuntimeException.class)
                    .hasMessageContaining("Ambiguous JACT method overload");
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class PageInvocationTestConfig {
        @Bean
        MessageService messageService() {
            return () -> "Resolved by Spring";
        }

        @Bean
        SamplePageBean samplePageBean() {
            return new SamplePageBean();
        }
    }

    public interface MessageService {
        String message();
    }

    public static class SamplePageBean {
        public JNode home(MessageService messageService) {
            return new TextNode(messageService.message());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ComponentInvocationTestConfig {
        @Bean
        MessageService messageService() {
            return () -> "Hello";
        }

        @Bean
        ComponentPageBean componentPageBean() {
            return new ComponentPageBean();
        }

        @Bean
        ComponentBean componentBean() {
            return new ComponentBean();
        }
    }

    public static class ComponentPageBean {
        public JNode home() {
            return Nodes.component("header", "Component");
        }
    }

    public static class ComponentBean {
        public JNode header(MessageService messageService, String label) {
            return new TextNode(messageService.message() + " " + label);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AmbiguousPageConfig {
        @Bean
        AmbiguousPageBean ambiguousPageBean() {
            return new AmbiguousPageBean();
        }
    }

    public static class AmbiguousPageBean {
        public JNode home() {
            return new TextNode("one");
        }

        public JNode home(MessageService messageService) {
            return new TextNode(messageService.message());
        }
    }

    private static RendererBridge noOpRenderer() {
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

    private static RendererBridge recordingRenderer(AtomicReference<JNode> mountedNode) {
        return new RendererBridge() {
            @Override
            public void ensureStarted() {
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
    }
}
