package io.jact.spring.boot;

import io.jact.annotations.JNode;
import io.jact.core.api.Navigator;
import io.jact.core.meta.ComponentDescriptor;
import io.jact.core.meta.PageDescriptor;
import io.jact.core.node.TextNode;
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
}
