package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.Arrays;
import java.util.List;
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

    public static CheckboxNode checkbox(String label, boolean checked, Consumer<Boolean> onChange) {
        return new CheckboxNode(label, checked, onChange);
    }

    public static SelectNode select(String value, List<String> options, Consumer<String> onChange) {
        return new SelectNode(value, options, onChange);
    }

    public static ComponentNode component(String componentId, Object... arguments) {
        return new ComponentNode(componentId, Arrays.asList(arguments));
    }

    public static ComponentNode component(String componentId, List<Object> arguments) {
        return new ComponentNode(componentId, arguments);
    }

    public static KeyedNode key(String key, JNode child) {
        return new KeyedNode(key, child);
    }

    public static StyledNode style(JNode child, NodeStyle style) {
        return new StyledNode(child, style);
    }

    public static StyledNode className(String className, JNode child) {
        return style(child, NodeStyle.className(className));
    }

    public static ScrollAreaNode scrollArea(JNode child) {
        return new ScrollAreaNode(child);
    }

    public static ContainerNode column(JNode... children) {
        return new ContainerNode(Arrays.asList(children));
    }

    public static RowNode row(JNode... children) {
        return new RowNode(Arrays.asList(children));
    }

    public static SpacerNode spacer() {
        return new SpacerNode();
    }

    public static DividerNode divider() {
        return new DividerNode();
    }
}
