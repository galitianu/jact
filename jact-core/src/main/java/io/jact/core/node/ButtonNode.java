package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.Objects;

public record ButtonNode(String label, Runnable onClick) implements JNode {
    public ButtonNode {
        label = Objects.requireNonNull(label, "label");
        onClick = Objects.requireNonNull(onClick, "onClick");
    }
}
