package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.Arrays;
import java.util.function.Consumer;

public final class Nodes {
    private Nodes() {
    }

    public static TextNode text(String value) {
        return new TextNode(value);
    }

    public static ButtonNode button(String label, Runnable onClick) {
        return new ButtonNode(label, onClick);
    }

    public static TextInputNode input(String value, String placeholder, Consumer<String> onChange) {
        return new TextInputNode(value, placeholder, onChange);
    }

    public static KeyedNode key(String key, JNode child) {
        return new KeyedNode(key, child);
    }

    public static ContainerNode column(JNode... children) {
        return new ContainerNode(Arrays.asList(children));
    }
}
