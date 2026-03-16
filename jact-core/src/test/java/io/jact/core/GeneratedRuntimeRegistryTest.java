package io.jact.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeneratedRuntimeRegistryTest {
    @Test
    void loadsGeneratedRegistriesWhenPresent() {
        GeneratedRuntimeRegistry registry = GeneratedRuntimeRegistry.from(getClass().getClassLoader());

        assertThat(registry.components())
            .hasSize(1)
            .first()
            .extracting("beanClassName", "methodName")
            .containsExactly("io.jact.sample.SampleComponents", "header");

        assertThat(registry.pages())
            .hasSize(1)
            .first()
            .extracting("routeTemplate", "beanClassName", "methodName")
            .containsExactly("/", "io.jact.sample.SamplePages", "home");
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
