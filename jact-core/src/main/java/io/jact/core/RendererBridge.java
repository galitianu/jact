package io.jact.core;

import io.jact.annotations.JNode;

public interface RendererBridge {
    void ensureStarted();

    void mount(JNode rootNode, WindowSettings windowSettings);
}
