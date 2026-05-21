package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.Objects;

public record ScrollAreaNode(JNode child) implements JNode {
    public ScrollAreaNode {
        child = Objects.requireNonNull(child, "child");
    }
}
