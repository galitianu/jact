package io.jact.core.runtime.spi;

import io.jact.annotations.JNode;
import io.jact.core.runtime.WindowSettings;

public interface RendererBridge {
    void ensureStarted();

    void mount(JNode rootNode, WindowSettings windowSettings);

    void update(JNode rootNode);

    void executeOnUiThread(Runnable task);
}
