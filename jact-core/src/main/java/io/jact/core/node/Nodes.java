package io.jact.core.node;

import io.jact.annotations.JNode;
import java.util.Arrays;

public final class Nodes {
    private Nodes() {
    }

    public static TextNode text(String value) {
        return new TextNode(value);
    }

    public static ContainerNode column(JNode... children) {
        return new ContainerNode(Arrays.asList(children));
    }
}
