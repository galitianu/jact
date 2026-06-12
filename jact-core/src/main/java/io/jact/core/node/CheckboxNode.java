package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.Objects;
import java.util.function.Consumer;

public record CheckboxNode(String label, boolean checked, Consumer<Boolean> onChange) implements JNode {
    public CheckboxNode {
        label = Objects.requireNonNull(label, "label");
        onChange = Objects.requireNonNull(onChange, "onChange");
    }
}
