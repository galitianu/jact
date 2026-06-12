package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.List;

public record RowNode(List<JNode> children) implements JNode {
    public RowNode {
        children = List.copyOf(children);
    }
}
