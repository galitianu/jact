package io.jact.core.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RouteParams {
    private static final RouteParams EMPTY = new RouteParams(Collections.emptyMap());

    private final Map<String, String> values;

    private RouteParams(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public static RouteParams empty() {
        return EMPTY;
    }

    public static RouteParams of(Map<String, String> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return empty();
        }
        return new RouteParams(new LinkedHashMap<>(values));
    }

    public String get(String key) {
        return values.get(key);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<String, String> asMap() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
