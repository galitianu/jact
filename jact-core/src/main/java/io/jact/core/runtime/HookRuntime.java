package io.jact.core.runtime;

import io.jact.core.api.Hooks;
import io.jact.core.api.Navigator;
import io.jact.core.api.State;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.routing.RouteParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

final class HookRuntime {
    private final Map<String, ComponentState> componentStates = new ConcurrentHashMap<>();
    private final Runnable scheduleRender;

    HookRuntime(Runnable scheduleRender) {
        this.scheduleRender = scheduleRender;
    }

    RenderSession beginRender(String componentKey, RouteParams routeParams, Navigator navigator) {
        ComponentState state = componentStates.computeIfAbsent(componentKey, ignored -> new ComponentState());
        RenderCursor cursor = new RenderCursor(state, routeParams, navigator, scheduleRender);
        Hooks.enter(cursor);
        return new RenderSession(cursor);
    }

    Runnable endRender(RenderSession session) {
        Hooks.exit();
        return session.cursor()::runPostCommitEffects;
    }

    void abortRender() {
        Hooks.exit();
    }

    void unmount(String componentKey) {
        ComponentState state = componentStates.remove(componentKey);
        if (state == null) {
            return;
        }

        synchronized (state) {
            for (HookSlot slot : state.slots) {
                if (slot instanceof EffectSlot effectSlot && effectSlot.cleanup != null) {
                    effectSlot.cleanup.run();
                    effectSlot.cleanup = null;
                }
            }
        }
    }

    private static boolean depsChanged(Object[] previous, Object[] next) {
        return !Arrays.deepEquals(previous, next);
    }

    private static Object[] normalizeDeps(Object[] deps) {
        return deps == null ? new Object[0] : Arrays.copyOf(deps, deps.length);
    }

    private static final class ComponentState {
        private final List<HookSlot> slots = new ArrayList<>();
    }

    private sealed interface HookSlot permits StateSlot, MemoSlot, EffectSlot {
    }

    private static final class StateSlot<T> implements HookSlot {
        private final StateHandle<T> handle;

        private StateSlot(StateHandle<T> handle) {
            this.handle = handle;
        }
    }

    private static final class MemoSlot implements HookSlot {
        private Object[] deps = new Object[0];
        private Object value;
        private boolean initialized;
    }

    private static final class EffectSlot implements HookSlot {
        private Object[] deps = new Object[0];
        private Hooks.Effect effect;
        private Hooks.Cleanup cleanup;
        private boolean initialized;
    }

    private static final class StateHandle<T> implements State<T> {
        private final AtomicReference<T> value;
        private final Runnable scheduleRender;

        private StateHandle(T initialValue, Runnable scheduleRender) {
            this.value = new AtomicReference<>(initialValue);
            this.scheduleRender = scheduleRender;
        }

        @Override
        public T get() {
            return value.get();
        }

        @Override
        public void set(T nextValue) {
            T previous = value.getAndSet(nextValue);
            if (!Objects.equals(previous, nextValue)) {
                scheduleRender.run();
            }
        }
    }

    private static final class RenderCursor implements Hooks.HookExecutionContext {
        private final ComponentState componentState;
        private final RouteParams routeParams;
        private final Navigator navigator;
        private final Runnable scheduleRender;
        private final List<Runnable> postCommitEffects = new ArrayList<>();
        private int nextSlotIndex;

        private RenderCursor(ComponentState componentState, RouteParams routeParams, Navigator navigator, Runnable scheduleRender) {
            this.componentState = componentState;
            this.routeParams = routeParams;
            this.navigator = navigator;
            this.scheduleRender = scheduleRender;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> State<T> useState(T initialValue) {
            synchronized (componentState) {
                HookSlot slot = getOrCreateSlot(nextSlotIndex++, () -> new StateSlot<>(new StateHandle<>(initialValue, scheduleRender)));
                if (!(slot instanceof StateSlot<?> stateSlot)) {
                    throw new JactRuntimeException("Hook slot mismatch for useState");
                }
                return (State<T>) stateSlot.handle;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T useMemo(Supplier<T> supplier, Object... deps) {
            synchronized (componentState) {
                HookSlot slot = getOrCreateSlot(nextSlotIndex++, MemoSlot::new);
                if (!(slot instanceof MemoSlot memoSlot)) {
                    throw new JactRuntimeException("Hook slot mismatch for useMemo");
                }

                Object[] normalized = normalizeDeps(deps);
                if (!memoSlot.initialized || depsChanged(memoSlot.deps, normalized)) {
                    memoSlot.value = supplier.get();
                    memoSlot.deps = normalized;
                    memoSlot.initialized = true;
                }

                return (T) memoSlot.value;
            }
        }

        @Override
        public void useEffect(Hooks.Effect effect, Object... deps) {
            synchronized (componentState) {
                HookSlot slot = getOrCreateSlot(nextSlotIndex++, EffectSlot::new);
                if (!(slot instanceof EffectSlot effectSlot)) {
                    throw new JactRuntimeException("Hook slot mismatch for useEffect");
                }

                Object[] normalized = normalizeDeps(deps);
                boolean shouldRun = !effectSlot.initialized || depsChanged(effectSlot.deps, normalized);
                effectSlot.effect = effect;
                effectSlot.deps = normalized;
                effectSlot.initialized = true;

                if (shouldRun) {
                    postCommitEffects.add(() -> {
                        if (effectSlot.cleanup != null) {
                            effectSlot.cleanup.run();
                            effectSlot.cleanup = null;
                        }
                        Hooks.Cleanup cleanup = effectSlot.effect.run();
                        effectSlot.cleanup = cleanup;
                    });
                }
            }
        }

        @Override
        public RouteParams routeParams() {
            return routeParams;
        }

        @Override
        public Navigator navigator() {
            return navigator;
        }

        private void runPostCommitEffects() {
            for (Runnable effect : postCommitEffects) {
                effect.run();
            }
        }

        private HookSlot getOrCreateSlot(int slotIndex, Supplier<HookSlot> factory) {
            if (slotIndex < componentState.slots.size()) {
                return componentState.slots.get(slotIndex);
            }

            HookSlot slot = factory.get();
            componentState.slots.add(slot);
            return slot;
        }
    }

    record RenderSession(RenderCursor cursor) {
    }
}
