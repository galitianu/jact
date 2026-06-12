package io.jact.core.api;

import io.jact.core.internal.JactRuntimeException;
import io.jact.core.routing.RouteParams;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;
import java.util.Objects;
import java.util.function.Supplier;

public final class Hooks {
    private static final ThreadLocal<Deque<HookExecutionContext>> CURRENT_CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    private Hooks() {
    }

    public static <T> State<T> useState(T initialValue) {
        return requireContext().useState(initialValue);
    }

    public static <T> T useMemo(Supplier<T> supplier, Object... deps) {
        Objects.requireNonNull(supplier, "supplier");
        return requireContext().useMemo(supplier, deps);
    }

    public static void useEffect(Runnable effect, Object... deps) {
        Objects.requireNonNull(effect, "effect");
        useEffect(() -> {
            effect.run();
            return null;
        }, deps);
    }

    public static void useEffect(Effect effect, Object... deps) {
        Objects.requireNonNull(effect, "effect");
        requireContext().useEffect(effect, deps);
    }

    public static <T> T useStore(Store<T> store) {
        Objects.requireNonNull(store, "store");
        return requireContext().useStore(store);
    }

    public static <T, R> R useStore(Store<T> store, Function<T, R> selector) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(selector, "selector");
        return requireContext().useStore(store, selector);
    }

    public static <T> T useExternal(ObservableValue<T> source) {
        Objects.requireNonNull(source, "source");
        return requireContext().useExternal(source);
    }

    public static RouteParams routeParams() {
        return requireContext().routeParams();
    }

    public static Navigator navigator() {
        return requireContext().navigator();
    }

    // Internal runtime entrypoint; not intended for application code.
    public static void enter(HookExecutionContext context) {
        CURRENT_CONTEXTS.get().push(context);
    }

    // Internal runtime exitpoint; not intended for application code.
    public static void exit() {
        Deque<HookExecutionContext> contexts = CURRENT_CONTEXTS.get();
        if (!contexts.isEmpty()) {
            contexts.pop();
        }
        if (contexts.isEmpty()) {
            CURRENT_CONTEXTS.remove();
        }
    }

    private static HookExecutionContext requireContext() {
        Deque<HookExecutionContext> contexts = CURRENT_CONTEXTS.get();
        if (contexts.isEmpty()) {
            throw new JactRuntimeException("Hooks can only be called during a JACT render cycle.");
        }
        return contexts.peek();
    }

    @FunctionalInterface
    public interface Effect {
        Cleanup run();
    }

    @FunctionalInterface
    public interface Cleanup {
        void run();
    }

    public interface HookExecutionContext {
        <T> State<T> useState(T initialValue);

        <T> T useMemo(Supplier<T> supplier, Object... deps);

        void useEffect(Effect effect, Object... deps);

        <T> T useStore(Store<T> store);

        <T, R> R useStore(Store<T> store, Function<T, R> selector);

        <T> T useExternal(ObservableValue<T> source);

        RouteParams routeParams();

        Navigator navigator();
    }
}
