package io.jact.core.node;

import io.jact.annotations.JNode;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public record SelectNode(String value, List<String> options, Consumer<String> onChange) implements JNode {
    public SelectNode {
        value = value == null ? "" : value;
        options = List.copyOf(options);
        onChange = Objects.requireNonNull(onChange, "onChange");
    }
}
