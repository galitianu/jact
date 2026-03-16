package io.jact.core.node;

import io.jact.annotations.JNode;

public record TextNode(String value) implements JNode {
}
