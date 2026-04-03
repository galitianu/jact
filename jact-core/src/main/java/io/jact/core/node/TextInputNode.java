package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.Objects;
import java.util.function.Consumer;

public record TextInputNode(String value, String placeholder, Consumer<String> onChange) implements JNode {
    public TextInputNode {
        value = value == null ? "" : value;
        placeholder = placeholder == null ? "" : placeholder;
        onChange = Objects.requireNonNull(onChange, "onChange");
    }
}
