package io.jact.javafx.renderer;

import io.jact.annotations.JNode;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.node.ButtonNode;
import io.jact.core.node.ContainerNode;
import io.jact.core.node.TextNode;
import io.jact.core.runtime.WindowSettings;
import io.jact.core.runtime.spi.RendererBridge;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JavaFxRendererBridge implements RendererBridge {
    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile Stage stage;
    private volatile StackPane rootContainer;

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
        runOnFxAndWait(() -> {
            if (stage == null) {
                stage = new Stage();
                stage.setTitle(windowSettings.title());
                rootContainer = new StackPane();
                Scene scene = new Scene(rootContainer, windowSettings.width(), windowSettings.height());
                stage.setScene(scene);
                stage.show();
            }
            rootContainer.getChildren().setAll(toFxNode(rootNode));
        });
    }

    @Override
    public void update(JNode rootNode) {
        ensureStarted();
        runOnFxAndWait(() -> {
            if (rootContainer == null) {
                throw new JactRuntimeException("Cannot update renderer before initial mount.");
            }
            rootContainer.getChildren().setAll(toFxNode(rootNode));
        });
    }

    @Override
    public void executeOnUiThread(Runnable task) {
        ensureStarted();
        Platform.runLater(task);
    }

    private void runOnFxAndWait(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        final RuntimeException[] failure = new RuntimeException[1];

        Platform.runLater(() -> {
            try {
                action.run();
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
            throw new JactRuntimeException("Interrupted while waiting for JavaFX UI operation.", interruptedException);
        }

        if (failure[0] != null) {
            throw new JactRuntimeException("JavaFX UI operation failed.", failure[0]);
        }
    }

    private Node toFxNode(JNode node) {
        if (node instanceof TextNode(String value)) {
            return new Label(value);
        }

        if (node instanceof ButtonNode(String label, Runnable onClick)) {
            Button button = new Button(label);
            button.setOnAction(event -> onClick.run());
            return button;
        }

        if (node instanceof ContainerNode(List<JNode> children)) {
            VBox vBox = new VBox(8);
            for (JNode child : children) {
                vBox.getChildren().add(toFxNode(child));
            }
            return vBox;
        }

        return new Label(node.getClass().getSimpleName());
    }
}
