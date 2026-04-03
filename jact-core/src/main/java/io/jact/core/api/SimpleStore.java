package io.jact.core.api;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public final class SimpleStore<T> implements Store<T> {
    private final AtomicReference<T> value;
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    public SimpleStore(T initialValue) {
        this.value = new AtomicReference<>(initialValue);
    }

    @Override
    public T get() {
        return value.get();
    }

    @Override
    public void set(T nextValue) {
        T previous = value.getAndSet(nextValue);
        if (!Objects.equals(previous, nextValue)) {
            notifyListeners();
        }
    }

    @Override
    public void update(UnaryOperator<T> updater) {
        Objects.requireNonNull(updater, "updater");

        while (true) {
            T previous = value.get();
            T next = updater.apply(previous);
            if (value.compareAndSet(previous, next)) {
                if (!Objects.equals(previous, next)) {
                    notifyListeners();
                }
                return;
            }
        }
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
