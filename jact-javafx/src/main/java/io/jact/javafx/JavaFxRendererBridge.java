package io.jact.javafx;

import io.jact.annotations.JNode;
import io.jact.core.JactRuntimeException;
import io.jact.core.RendererBridge;
import io.jact.core.WindowSettings;
import io.jact.core.node.ContainerNode;
import io.jact.core.node.TextNode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class JavaFxRendererBridge implements RendererBridge {
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void ensureStarted() {
        if (started.get()) {
            return;
        }

        try {
            Platform.startup(() -> {
            });
            started.set(true);
        } catch (IllegalStateException alreadyStarted) {
            started.set(true);
        } catch (RuntimeException exception) {
            throw new JactRuntimeException("Failed to initialize JavaFX platform", exception);
        }
    }

    @Override
    public void mount(JNode rootNode, WindowSettings windowSettings) {
        ensureStarted();

        CountDownLatch latch = new CountDownLatch(1);
        final RuntimeException[] failure = new RuntimeException[1];

        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                stage.setTitle(windowSettings.title());
                Scene scene = new Scene(new StackPane(toFxNode(rootNode)), windowSettings.width(), windowSettings.height());
                stage.setScene(scene);
                stage.show();
            } catch (RuntimeException exception) {
                failure[0] = exception;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new JactRuntimeException("Interrupted while mounting initial JavaFX scene", interruptedException);
        }

        if (failure[0] != null) {
            throw new JactRuntimeException("Failed to mount JavaFX scene", failure[0]);
        }
    }

    private javafx.scene.Node toFxNode(JNode node) {
        if (node instanceof TextNode textNode) {
            return new Label(textNode.value());
        }

        if (node instanceof ContainerNode containerNode) {
            VBox vBox = new VBox(8);
            for (JNode child : containerNode.children()) {
                vBox.getChildren().add(toFxNode(child));
            }
            return vBox;
        }

        return new Label(node.getClass().getSimpleName());
    }
}
