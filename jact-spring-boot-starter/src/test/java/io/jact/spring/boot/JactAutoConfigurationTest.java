package io.jact.spring.boot;

import io.jact.annotations.JNode;
import io.jact.core.api.Navigator;
import io.jact.core.registry.RuntimeRegistry;
import io.jact.core.runtime.JactRuntime;
import io.jact.core.runtime.WindowSettings;
import io.jact.core.runtime.spi.RendererBridge;
import io.jact.spring.boot.autoconfigure.JactAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
}
