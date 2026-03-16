package io.jact.core;

import io.jact.annotations.JNode;
import io.jact.core.descriptor.PageDescriptor;

public final class JactRuntime {
    private final RuntimeRegistry runtimeRegistry;
    private final RendererBridge rendererBridge;

    public JactRuntime(RuntimeRegistry runtimeRegistry, RendererBridge rendererBridge) {
        this.runtimeRegistry = runtimeRegistry;
        this.rendererBridge = rendererBridge;
    }

    public void start() {
        rendererBridge.ensureStarted();
    }

    public void mountInitialPage(String initialRoute, PageResolver pageResolver, WindowSettings windowSettings) {
        PageDescriptor pageDescriptor = runtimeRegistry.pages().stream()
            .filter(page -> page.routeTemplate().equals(initialRoute))
            .findFirst()
            .orElseThrow(() -> new JactRuntimeException("No page found for route: " + initialRoute));

        JNode rootNode = pageResolver.resolve(pageDescriptor);
        rendererBridge.mount(rootNode, windowSettings);
    }
}
