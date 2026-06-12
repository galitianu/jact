package io.jact.core.runtime;

import io.jact.annotations.JNode;
import io.jact.core.api.Navigator;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.meta.PageDescriptor;
import io.jact.core.meta.ComponentDescriptor;
import io.jact.core.node.ComponentNode;
import io.jact.core.node.ContainerNode;
import io.jact.core.node.KeyedNode;
import io.jact.core.node.RowNode;
import io.jact.core.node.ScrollAreaNode;
import io.jact.core.node.StyledNode;
import io.jact.core.registry.RuntimeRegistry;
import io.jact.core.routing.RouteParams;
import io.jact.core.routing.RouteTemplate;
import io.jact.core.runtime.spi.ComponentResolver;
import io.jact.core.runtime.spi.PageResolver;
import io.jact.core.runtime.spi.RendererBridge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private ComponentResolver componentResolver;
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
        mountInitialPage(initialRoute, pageResolver, request -> {
            throw new JactRuntimeException("No component resolver configured for component: " + request.componentId());
        }, windowSettings);
    }

    public void mountInitialPage(
        String initialRoute,
        PageResolver pageResolver,
        ComponentResolver componentResolver,
        WindowSettings windowSettings
    ) {
        Objects.requireNonNull(pageResolver, "pageResolver");
        Objects.requireNonNull(componentResolver, "componentResolver");
        Objects.requireNonNull(windowSettings, "windowSettings");

        synchronized (lifecycleLock) {
            this.pageResolver = pageResolver;
            this.componentResolver = componentResolver;
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
        ComponentResolver components;
        WindowSettings settings;
        boolean shouldMount;

        synchronized (lifecycleLock) {
            context = routeContext;
            resolver = pageResolver;
            components = componentResolver;
            settings = windowSettings;
            shouldMount = !mounted;
        }

        if (context == null || resolver == null || components == null || settings == null) {
            throw new JactRuntimeException("Cannot render without route context and initialization settings.");
        }

        String pageIdentity = pageKey(context.pageDescriptor());
        Set<String> activeIdentities = new LinkedHashSet<>();
        List<Runnable> postCommitTasks = new ArrayList<>();
        activeIdentities.add(pageIdentity);
        HookRuntime.RenderSession renderSession = hookRuntime.beginRender(pageIdentity, context.params(), navigator);

        JNode rootNode;
        try {
            rootNode = resolver.resolve(new RenderRequest(context.path(), context.pageDescriptor(), context.params(), navigator));
            rootNode = resolveComponents(rootNode, components, context, pageIdentity, "root", activeIdentities, postCommitTasks);
            postCommitTasks.add(hookRuntime.endRender(renderSession));
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

        hookRuntime.retainOnly(activeIdentities);
        for (Runnable postCommitTask : postCommitTasks) {
            postCommitTask.run();
        }
    }

    private JNode resolveComponents(
        JNode node,
        ComponentResolver resolver,
        RouteContext routeContext,
        String parentIdentity,
        String identitySegment,
        Set<String> activeIdentities,
        List<Runnable> postCommitTasks
    ) {
        Objects.requireNonNull(node, "node");

        if (node instanceof ComponentNode componentNode) {
            ComponentDescriptor descriptor = resolveComponentDescriptor(componentNode.componentId());
            String componentIdentity = componentKey(parentIdentity, identitySegment, descriptor);
            activeIdentities.add(componentIdentity);
            HookRuntime.RenderSession renderSession = hookRuntime.beginRender(componentIdentity, routeContext.params(), navigator);
            try {
                JNode resolved = resolver.resolve(new ComponentRequest(
                    componentNode.componentId(),
                    descriptor,
                    componentNode.arguments(),
                    routeContext.path(),
                    routeContext.params(),
                    navigator
                ));
                JNode resolvedTree = resolveComponents(
                    resolved,
                    resolver,
                    routeContext,
                    componentIdentity,
                    "root",
                    activeIdentities,
                    postCommitTasks
                );
                postCommitTasks.add(hookRuntime.endRender(renderSession));
                return resolvedTree;
            } catch (RuntimeException exception) {
                hookRuntime.abortRender();
                throw exception;
            }
        }

        if (node instanceof KeyedNode keyedNode) {
            return new KeyedNode(
                keyedNode.key(),
                resolveComponents(
                    keyedNode.child(),
                    resolver,
                    routeContext,
                    parentIdentity,
                    "key:" + keyedNode.key(),
                    activeIdentities,
                    postCommitTasks
                )
            );
        }

        if (node instanceof StyledNode styledNode) {
            return new StyledNode(
                resolveComponents(
                    styledNode.child(),
                    resolver,
                    routeContext,
                    parentIdentity,
                    identitySegment,
                    activeIdentities,
                    postCommitTasks
                ),
                styledNode.style()
            );
        }

        if (node instanceof ContainerNode containerNode) {
            List<JNode> children = new ArrayList<>(containerNode.children().size());
            for (int i = 0; i < containerNode.children().size(); i++) {
                children.add(resolveComponents(
                    containerNode.children().get(i),
                    resolver,
                    routeContext,
                    parentIdentity,
                    "index:" + i,
                    activeIdentities,
                    postCommitTasks
                ));
            }
            return new ContainerNode(children);
        }

        if (node instanceof RowNode rowNode) {
            List<JNode> children = new ArrayList<>(rowNode.children().size());
            for (int i = 0; i < rowNode.children().size(); i++) {
                children.add(resolveComponents(
                    rowNode.children().get(i),
                    resolver,
                    routeContext,
                    parentIdentity,
                    "row-index:" + i,
                    activeIdentities,
                    postCommitTasks
                ));
            }
            return new RowNode(children);
        }

        if (node instanceof ScrollAreaNode scrollAreaNode) {
            return new ScrollAreaNode(resolveComponents(
                scrollAreaNode.child(),
                resolver,
                routeContext,
                parentIdentity,
                "scroll-child",
                activeIdentities,
                postCommitTasks
            ));
        }

        return node;
    }

    private ComponentDescriptor resolveComponentDescriptor(String componentId) {
        List<ComponentDescriptor> matches = runtimeRegistry.components().stream()
            .filter(descriptor -> componentMatches(descriptor, componentId))
            .toList();

        if (matches.isEmpty()) {
            throw new JactRuntimeException("No component found for id: " + componentId);
        }
        if (matches.size() > 1) {
            throw new JactRuntimeException("Ambiguous component id '" + componentId + "'. Use beanClass#method.");
        }
        return matches.getFirst();
    }

    private boolean componentMatches(ComponentDescriptor descriptor, String componentId) {
        String qualifiedId = descriptor.beanClassName() + "#" + descriptor.methodName();
        String simpleClassName = descriptor.beanClassName().substring(descriptor.beanClassName().lastIndexOf('.') + 1);
        String simpleQualifiedId = simpleClassName + "#" + descriptor.methodName();
        return componentId.equals(descriptor.componentId())
            || componentId.equals(qualifiedId)
            || componentId.equals(simpleQualifiedId)
            || componentId.equals(descriptor.methodName());
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
        return "page:" + descriptor.beanClassName() + "#" + descriptor.methodName();
    }

    private String componentKey(String parentIdentity, String identitySegment, ComponentDescriptor descriptor) {
        return parentIdentity
            + "/" + identitySegment
            + ":component:" + descriptor.beanClassName()
            + "#" + descriptor.methodName();
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
