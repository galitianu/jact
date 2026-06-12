package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.Objects;

public record StyledNode(JNode child, NodeStyle style) implements JNode {
    public StyledNode {
        child = Objects.requireNonNull(child, "child");
        style = style == null ? NodeStyle.empty() : style;
    }
}
