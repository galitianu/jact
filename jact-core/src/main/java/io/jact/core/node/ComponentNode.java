package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.List;
import java.util.Objects;

public record ComponentNode(String componentId, List<Object> arguments) implements JNode {
    public ComponentNode {
        componentId = Objects.requireNonNull(componentId, "componentId");
        if (componentId.isBlank()) {
            throw new IllegalArgumentException("componentId must not be blank");
        }
        arguments = List.copyOf(arguments);
    }
}
