package io.jact.core.api;

public interface ObservableValue<T> {
    T get();

    Subscription subscribe(Runnable listener);
}
