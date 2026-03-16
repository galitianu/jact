package io.jact.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import io.jact.core.JactRuntime;
import io.jact.core.RendererBridge;
import io.jact.core.RuntimeRegistry;
import io.jact.core.WindowSettings;
import io.jact.annotations.JNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
            })
            .run(context -> {
                assertThat(context).hasSingleBean(RuntimeRegistry.class);
                assertThat(context).hasSingleBean(JactRuntime.class);
                assertThat(context).hasSingleBean(ApplicationRunner.class);
            });
    }
}
