package io.jact.core.runtime;

import io.jact.annotations.JNode;
import io.jact.core.api.Navigator;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.meta.PageDescriptor;
import io.jact.core.registry.RuntimeRegistry;
import io.jact.core.routing.RouteParams;
import io.jact.core.routing.RouteTemplate;
import io.jact.core.runtime.spi.PageResolver;
import io.jact.core.runtime.spi.RendererBridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JactRuntime {
    private final RuntimeRegistry runtimeRegistry;
    private final RendererBridge rendererBridge;
    private final HookRuntime hookRuntime;
    private final Navigator navigator;
    private final AtomicBoolean renderScheduled = new AtomicBoolean(false);
    private final AtomicBoolean renderPending = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();

    private final List<String> navigationHistory = new ArrayList<>();
    private int historyIndex = -1;

    private PageResolver pageResolver;
    private WindowSettings windowSettings;
    private RouteContext routeContext;
    private boolean mounted;

    public JactRuntime(RuntimeRegistry runtimeRegistry, RendererBridge rendererBridge) {
        this.runtimeRegistry = runtimeRegistry;
        this.rendererBridge = rendererBridge;
        this.hookRuntime = new HookRuntime(this::scheduleRender);
        this.navigator = new RuntimeNavigator();
    }

    public void start() {
        rendererBridge.ensureStarted();
    }

    public Navigator navigator() {
        return navigator;
    }

    public void mountInitialPage(String initialRoute, PageResolver pageResolver, WindowSettings windowSettings) {
        Objects.requireNonNull(pageResolver, "pageResolver");
        Objects.requireNonNull(windowSettings, "windowSettings");

        synchronized (lifecycleLock) {
            this.pageResolver = pageResolver;
            this.windowSettings = windowSettings;
        }

        navigate(RouteTemplate.normalizePath(initialRoute), NavigationMode.REPLACE, false);
    }

    private void scheduleRender() {
        if (!renderScheduled.compareAndSet(false, true)) {
            renderPending.set(true);
            return;
        }

        rendererBridge.executeOnUiThread(this::drainRenderQueue);
    }

    private void drainRenderQueue() {
        try {
            do {
                renderPending.set(false);
                renderCurrent();
            } while (renderPending.getAndSet(false));
        } finally {
            renderScheduled.set(false);
            if (renderPending.getAndSet(false)) {
                scheduleRender();
            }
        }
    }

    private void navigate(String rawPath, NavigationMode mode, boolean fromNavigatorBack) {
        RouteContext previous;
        RouteContext next;

        synchronized (lifecycleLock) {
            if (pageResolver == null || windowSettings == null) {
                throw new JactRuntimeException("JACT runtime is not initialized. Call mountInitialPage() first.");
            }

            String normalizedPath = RouteTemplate.normalizePath(rawPath);
            next = resolveRoute(normalizedPath);
            previous = routeContext;
            routeContext = next;

            if (mode == NavigationMode.REPLACE) {
                if (historyIndex < 0) {
                    navigationHistory.add(normalizedPath);
                    historyIndex = 0;
                } else {
                    navigationHistory.set(historyIndex, normalizedPath);
                }
            } else if (mode == NavigationMode.PUSH) {
                if (historyIndex < navigationHistory.size() - 1) {
                    navigationHistory.subList(historyIndex + 1, navigationHistory.size()).clear();
                }
                navigationHistory.add(normalizedPath);
                historyIndex = navigationHistory.size() - 1;
            } else if (mode == NavigationMode.BACK && !fromNavigatorBack) {
                throw new JactRuntimeException("Back navigation mode can only be used internally.");
            }
        }

        String previousKey = previous == null ? null : pageKey(previous.pageDescriptor());
        String nextKey = pageKey(next.pageDescriptor());
        if (previousKey != null && !previousKey.equals(nextKey)) {
            hookRuntime.unmount(previousKey);
        }

        renderCurrent();
    }

    private void renderCurrent() {
        RouteContext context;
        PageResolver resolver;
        WindowSettings settings;
        boolean shouldMount;

        synchronized (lifecycleLock) {
            context = routeContext;
            resolver = pageResolver;
            settings = windowSettings;
            shouldMount = !mounted;
        }

        if (context == null || resolver == null || settings == null) {
            throw new JactRuntimeException("Cannot render without route context and initialization settings.");
        }

        String componentKey = pageKey(context.pageDescriptor());
        HookRuntime.RenderSession renderSession = hookRuntime.beginRender(componentKey, context.params(), navigator);

        JNode rootNode;
        Runnable postCommit;
        try {
            rootNode = resolver.resolve(new RenderRequest(context.path(), context.pageDescriptor(), context.params(), navigator));
            postCommit = hookRuntime.endRender(renderSession);
        } catch (RuntimeException exception) {
            hookRuntime.abortRender();
            throw exception;
        }

        if (shouldMount) {
            rendererBridge.mount(rootNode, settings);
            synchronized (lifecycleLock) {
                mounted = true;
            }
        } else {
            rendererBridge.update(rootNode);
        }

        postCommit.run();
    }

    private RouteContext resolveRoute(String path) {
        RouteMatch bestMatch = null;
        for (PageDescriptor descriptor : runtimeRegistry.pages()) {
            RouteMatch candidate = match(descriptor, path);
            if (candidate == null) {
                continue;
            }

            if (bestMatch == null || candidate.staticSegmentCount() > bestMatch.staticSegmentCount()) {
                bestMatch = candidate;
            }
        }

        if (bestMatch == null) {
            throw new JactRuntimeException("No page found for route: " + path);
        }

        return new RouteContext(path, bestMatch.pageDescriptor(), RouteParams.of(bestMatch.params()));
    }

    private RouteMatch match(PageDescriptor descriptor, String concretePath) {
        String normalizedTemplate = RouteTemplate.normalizePath(descriptor.routeTemplate());
        String normalizedPath = RouteTemplate.normalizePath(concretePath);

        if ("/".equals(normalizedTemplate) && "/".equals(normalizedPath)) {
            return new RouteMatch(descriptor, Map.of(), 1);
        }

        String[] templateSegments = splitSegments(normalizedTemplate);
        String[] pathSegments = splitSegments(normalizedPath);
        if (templateSegments.length != pathSegments.length) {
            return null;
        }

        Map<String, String> params = new LinkedHashMap<>();
        int staticSegments = 0;
        for (int i = 0; i < templateSegments.length; i++) {
            String templateSegment = templateSegments[i];
            String concreteSegment = pathSegments[i];

            if (templateSegment.startsWith("$") && templateSegment.length() > 1) {
                params.put(templateSegment.substring(1), concreteSegment);
            } else if (templateSegment.equals(concreteSegment)) {
                staticSegments++;
            } else {
                return null;
            }
        }

        return new RouteMatch(descriptor, params, staticSegments);
    }

    private String[] splitSegments(String path) {
        if ("/".equals(path)) {
            return new String[0];
        }
        return path.substring(1).split("/");
    }

    private String pageKey(PageDescriptor descriptor) {
        return descriptor.beanClassName() + "#" + descriptor.methodName();
    }

    private enum NavigationMode {
        PUSH,
        REPLACE,
        BACK
    }

    private record RouteContext(String path, PageDescriptor pageDescriptor, RouteParams params) {
    }

    private record RouteMatch(PageDescriptor pageDescriptor, Map<String, String> params, int staticSegmentCount) {
    }

    private final class RuntimeNavigator implements Navigator {
        @Override
        public void push(String path) {
            navigate(path, NavigationMode.PUSH, false);
        }

        @Override
        public void replace(String path) {
            navigate(path, NavigationMode.REPLACE, false);
        }

        @Override
        public boolean back() {
            String target;
            synchronized (lifecycleLock) {
                if (historyIndex <= 0) {
                    return false;
                }
                historyIndex -= 1;
                target = navigationHistory.get(historyIndex);
            }
            navigate(target, NavigationMode.BACK, true);
            return true;
        }

        @Override
        public String currentPath() {
            synchronized (lifecycleLock) {
                return routeContext == null ? "/" : routeContext.path();
            }
        }

        @Override
        public RouteParams currentParams() {
            synchronized (lifecycleLock) {
                return routeContext == null ? RouteParams.empty() : routeContext.params();
            }
        }
    }
}
