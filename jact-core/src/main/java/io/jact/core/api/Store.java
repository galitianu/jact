package io.jact.core.api;

import java.util.Objects;
import java.util.function.UnaryOperator;

public interface Store<T> extends ObservableValue<T> {
    void set(T value);

    default void update(UnaryOperator<T> updater) {
        Objects.requireNonNull(updater, "updater");
        set(updater.apply(get()));
    }
}
