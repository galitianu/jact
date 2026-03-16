package io.jact.core.node;

import io.jact.annotations.JNode;
import java.util.List;

public record ContainerNode(List<JNode> children) implements JNode {
    public ContainerNode {
        children = List.copyOf(children);
    }
}
