package io.jact.spring.boot.bridge;

import io.jact.core.api.ObservableValue;
import io.jact.core.api.SimpleStore;
import io.jact.core.api.Store;
import io.jact.core.api.Subscription;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Helper for Spring services that want to expose externally-changing values
 * through JACT's observable contract.
 */
public final class ServiceStateBridge<T> implements ObservableValue<T> {
    private final Store<T> store;

    public ServiceStateBridge(T initialValue) {
        this.store = new SimpleStore<>(initialValue);
    }

    @Override
    public T get() {
        return store.get();
    }

    public void set(T value) {
        store.set(value);
    }

    public void update(UnaryOperator<T> updater) {
        Objects.requireNonNull(updater, "updater");
        store.update(updater);
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return store.subscribe(listener);
    }
}
