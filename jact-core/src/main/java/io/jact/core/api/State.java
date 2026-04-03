package io.jact.core.api;

public interface State<T> {
    T get();

    void set(T value);
}
