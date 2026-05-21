package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.Objects;

public record KeyedNode(String key, JNode child) implements JNode {
    public KeyedNode {
        key = Objects.requireNonNull(key, "key").trim();
        child = Objects.requireNonNull(child, "child");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }
}
