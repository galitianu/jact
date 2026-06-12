package io.jact.core.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    public Optional<String> find(String key) {
        return Optional.ofNullable(get(key));
    }

    public int getInt(String key) {
        return findInt(key).orElseThrow(() -> missingOrInvalid(key, "int"));
    }

    public Optional<Integer> findInt(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public long getLong(String key) {
        return findLong(key).orElseThrow(() -> missingOrInvalid(key, "long"));
    }

    public Optional<Long> findLong(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public boolean getBoolean(String key) {
        return findBoolean(key).orElseThrow(() -> missingOrInvalid(key, "boolean"));
    }

    public Optional<Boolean> findBoolean(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        if ("true".equalsIgnoreCase(value)) {
            return Optional.of(true);
        }
        if ("false".equalsIgnoreCase(value)) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    private IllegalArgumentException missingOrInvalid(String key, String type) {
        return new IllegalArgumentException("Missing or invalid route parameter '" + key + "' as " + type + ".");
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
