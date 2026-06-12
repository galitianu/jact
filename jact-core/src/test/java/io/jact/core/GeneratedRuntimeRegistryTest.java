package io.jact.core;

import io.jact.core.registry.GeneratedRuntimeRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedRuntimeRegistryTest {
    @Test
    void loadsGeneratedRegistriesWhenPresent() {
        GeneratedRuntimeRegistry registry = GeneratedRuntimeRegistry.from(getClass().getClassLoader());

        assertThat(registry.components())
            .hasSize(1)
            .first()
            .extracting("componentId", "beanClassName", "methodName", "parameterTypeNames")
            .containsExactly(
                "io.jact.sample.SampleComponents#header",
                "io.jact.sample.SampleComponents",
                "header",
                java.util.List.of("java.lang.String")
            );

        assertThat(registry.pages())
            .hasSize(1)
            .first()
            .extracting("routeTemplate", "beanClassName", "methodName", "parameterTypeNames")
            .containsExactly(
                "/",
                "io.jact.sample.SamplePages",
                "home",
                java.util.List.of("io.jact.core.routing.RouteParams")
            );
    }

    @Test
    void returnsEmptyWhenRegistriesAreMissing() {
        ClassLoader emptyLoader = new ClassLoader(null) {
        };

        GeneratedRuntimeRegistry registry = GeneratedRuntimeRegistry.from(emptyLoader);
        assertThat(registry.components()).isEmpty();
        assertThat(registry.pages()).isEmpty();
    }
}
