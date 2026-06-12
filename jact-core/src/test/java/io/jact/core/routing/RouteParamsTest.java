package io.jact.core.routing;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteParamsTest {
    @Test
    void readsTypedValues() {
        RouteParams params = RouteParams.of(Map.of(
            "id", "42",
            "page", "3",
            "done", "true"
        ));

        assertThat(params.getLong("id")).isEqualTo(42L);
        assertThat(params.getInt("page")).isEqualTo(3);
        assertThat(params.getBoolean("done")).isTrue();
        assertThat(params.findLong("missing")).isEmpty();
    }

    @Test
    void failsClearlyForInvalidTypedValues() {
        RouteParams params = RouteParams.of(Map.of("id", "abc"));

        assertThatThrownBy(() -> params.getLong("id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Missing or invalid route parameter 'id' as long");
    }
}
