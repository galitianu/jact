package io.jact.core.api;

import io.jact.core.internal.JactRuntimeException;
import io.jact.core.routing.RouteParams;

import java.util.Objects;
import java.util.function.Supplier;

public final class Hooks {
    private static final ThreadLocal<HookExecutionContext> CURRENT_CONTEXT = new ThreadLocal<>();

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

    public static RouteParams routeParams() {
        return requireContext().routeParams();
    }

    public static Navigator navigator() {
        return requireContext().navigator();
    }

    // Internal runtime entrypoint; not intended for application code.
    public static void enter(HookExecutionContext context) {
        CURRENT_CONTEXT.set(context);
    }

    // Internal runtime exitpoint; not intended for application code.
    public static void exit() {
        CURRENT_CONTEXT.remove();
    }

    private static HookExecutionContext requireContext() {
        HookExecutionContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            throw new JactRuntimeException("Hooks can only be called during a JACT render cycle.");
        }
        return context;
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

        RouteParams routeParams();

        Navigator navigator();
    }
}
