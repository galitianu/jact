package io.jact.core;

import io.jact.annotations.JNode;
import io.jact.core.descriptor.PageDescriptor;
import io.jact.core.node.Nodes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JactRuntimeTest {
    @Test
    void mountsResolvedPageIntoRenderer() {
        RuntimeRegistry runtimeRegistry = new RuntimeRegistry() {
            @Override
            public List<io.jact.core.descriptor.ComponentDescriptor> components() {
                return List.of();
            }

            @Override
            public List<PageDescriptor> pages() {
                return List.of(new PageDescriptor("/", "io.jact.sample.SamplePages", "home"));
            }
        };

        AtomicBoolean started = new AtomicBoolean();
        AtomicReference<JNode> mountedNode = new AtomicReference<>();
        RendererBridge rendererBridge = new RendererBridge() {
            @Override
            public void ensureStarted() {
                started.set(true);
            }

            @Override
            public void mount(JNode rootNode, WindowSettings windowSettings) {
                mountedNode.set(rootNode);
            }
        };

        JactRuntime runtime = new JactRuntime(runtimeRegistry, rendererBridge);
        runtime.start();
        runtime.mountInitialPage("/", descriptor -> Nodes.text("ok"), new WindowSettings("demo", 800, 600));

        assertThat(started.get()).isTrue();
        assertThat(mountedNode.get()).isNotNull();
    }
}
