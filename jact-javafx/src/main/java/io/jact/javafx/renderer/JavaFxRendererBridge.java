package io.jact.javafx.renderer;

import io.jact.annotations.JNode;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.node.ButtonNode;
import io.jact.core.node.ContainerNode;
import io.jact.core.node.KeyedNode;
import io.jact.core.node.ScrollAreaNode;
import io.jact.core.node.TextInputNode;
import io.jact.core.node.TextNode;
import io.jact.core.runtime.WindowSettings;
import io.jact.core.runtime.spi.RendererBridge;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class JavaFxRendererBridge implements RendererBridge {
    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile Stage stage;
    private volatile StackPane rootContainer;
    private volatile RenderedNode rootRendered;

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

            rootRendered = createRenderedNode(normalize(rootNode));
            rootContainer.getChildren().setAll(rootRendered.fxNode);
        });
    }

    @Override
    public void update(JNode rootNode) {
        ensureStarted();
        runOnFxAndWait(() -> {
            if (rootContainer == null) {
                throw new JactRuntimeException("Cannot update renderer before initial mount.");
            }

            NodeSpec nextSpec = normalize(rootNode);
            if (rootRendered == null) {
                rootRendered = createRenderedNode(nextSpec);
            } else {
                rootRendered = reconcile(rootRendered, nextSpec);
            }

            if (rootContainer.getChildren().isEmpty() || rootContainer.getChildren().get(0) != rootRendered.fxNode) {
                rootContainer.getChildren().setAll(rootRendered.fxNode);
            }
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

    private RenderedNode reconcile(RenderedNode current, NodeSpec next) {
        NodeKind nextKind = kindOf(next.node());
        if (current.kind != nextKind) {
            return createRenderedNode(next);
        }

        current.key = next.key();
        switch (next.node()) {
            case TextNode(String value) -> {
                Label label = (Label) current.fxNode;
                if (!value.equals(label.getText())) {
                    label.setText(value);
                }
                return current;
            }
            case ButtonNode(String label, Runnable onClick) -> {
                Button button = (Button) current.fxNode;
                if (!label.equals(button.getText())) {
                    button.setText(label);
                }
                current.buttonClick = onClick;
                button.setOnAction(event -> current.buttonClick.run());
                return current;
            }
            case TextInputNode(String value, String placeholder, Consumer<String> onChange) -> {
                TextField textField = (TextField) current.fxNode;
                if (!placeholder.equals(textField.getPromptText())) {
                    textField.setPromptText(placeholder);
                }

                current.inputChange = onChange;
                patchInputValue(current, textField, value);
                return current;
            }
            case ContainerNode(List<JNode> children) -> {
                reconcileContainer(current, children);
                return current;
            }
            case ScrollAreaNode(JNode child) -> {
                ScrollPane scrollPane = (ScrollPane) current.fxNode;
                RenderedNode previousChild = current.children.isEmpty() ? null : current.children.get(0);
                NodeSpec nextChildSpec = normalize(child);

                RenderedNode nextRenderedChild;
                if (previousChild == null || previousChild.kind != kindOf(nextChildSpec.node())) {
                    nextRenderedChild = createRenderedNode(nextChildSpec);
                } else {
                    nextRenderedChild = reconcile(previousChild, nextChildSpec);
                }

                current.children = List.of(nextRenderedChild);
                if (scrollPane.getContent() != nextRenderedChild.fxNode) {
                    scrollPane.setContent(nextRenderedChild.fxNode);
                }
                return current;
            }
            default -> throw unsupportedNode(next.node());
        }
    }

    private void patchInputValue(RenderedNode holder, TextField textField, String nextValue) {
        String normalizedNext = nextValue == null ? "" : nextValue;
        if (normalizedNext.equals(textField.getText())) {
            return;
        }

        boolean focused = textField.isFocused();
        int caret = textField.getCaretPosition();
        IndexRange selection = textField.getSelection();

        holder.syncingInput = true;
        try {
            textField.setText(normalizedNext);
        } finally {
            holder.syncingInput = false;
        }

        if (!focused) {
            return;
        }

        int max = normalizedNext.length();
        int selectionStart = Math.min(selection.getStart(), max);
        int selectionEnd = Math.min(selection.getEnd(), max);
        int caretTarget = Math.min(caret, max);

        textField.requestFocus();
        if (selectionStart != selectionEnd) {
            textField.selectRange(selectionStart, selectionEnd);
        } else {
            textField.positionCaret(caretTarget);
        }
    }

    private void reconcileContainer(RenderedNode containerHolder, List<JNode> nextChildrenRaw) {
        VBox vBox = (VBox) containerHolder.fxNode;
        List<RenderedNode> previousChildren = containerHolder.children;
        List<NodeSpec> nextChildren = normalizeChildren(nextChildrenRaw);

        validateDuplicateKeys(nextChildren);

        Map<String, RenderedNode> oldKeyed = new LinkedHashMap<>();
        List<RenderedNode> oldUnkeyed = new ArrayList<>();
        for (RenderedNode child : previousChildren) {
            if (child.key != null) {
                oldKeyed.put(child.key, child);
            } else {
                oldUnkeyed.add(child);
            }
        }

        List<RenderedNode> merged = new ArrayList<>(nextChildren.size());
        int oldUnkeyedIndex = 0;
        for (NodeSpec nextChild : nextChildren) {
            RenderedNode nextRendered;
            if (nextChild.key() != null) {
                RenderedNode existing = oldKeyed.remove(nextChild.key());
                nextRendered = existing == null
                    ? createRenderedNode(nextChild)
                    : reconcile(existing, nextChild);
            } else {
                if (oldUnkeyedIndex < oldUnkeyed.size()) {
                    RenderedNode existing = oldUnkeyed.get(oldUnkeyedIndex++);
                    if (existing.kind == kindOf(nextChild.node())) {
                        nextRendered = reconcile(existing, nextChild);
                    } else {
                        // For unkeyed mismatches (for example route/page transitions),
                        // replace the node instead of failing hard.
                        nextRendered = createRenderedNode(nextChild);
                    }
                } else {
                    nextRendered = createRenderedNode(nextChild);
                }
            }
            nextRendered.key = nextChild.key();
            merged.add(nextRendered);
        }

        containerHolder.children = merged;
        List<Node> expectedOrder = merged.stream().map(child -> child.fxNode).toList();
        if (!sameOrder(vBox.getChildren(), expectedOrder)) {
            vBox.getChildren().setAll(expectedOrder);
        }
    }

    private void validateDuplicateKeys(List<NodeSpec> children) {
        Set<String> seen = new HashSet<>();
        for (NodeSpec child : children) {
            if (child.key() == null) {
                continue;
            }
            if (!seen.add(child.key())) {
                throw new JactRuntimeException("Duplicate sibling key '" + child.key() + "' detected. Keys must be unique among siblings.");
            }
        }
    }

    private List<NodeSpec> normalizeChildren(List<JNode> rawChildren) {
        List<NodeSpec> normalized = new ArrayList<>(rawChildren.size());
        for (JNode child : rawChildren) {
            normalized.add(normalize(child));
        }
        return normalized;
    }

    private boolean sameOrder(List<Node> current, List<Node> expected) {
        if (current.size() != expected.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i) != expected.get(i)) {
                return false;
            }
        }
        return true;
    }

    private RenderedNode createRenderedNode(NodeSpec spec) {
        return switch (spec.node()) {
            case TextNode(String value) -> new RenderedNode(spec.key(), NodeKind.TEXT, new Label(value));
            case ButtonNode(String label, Runnable onClick) -> {
                Button button = new Button(label);
                RenderedNode rendered = new RenderedNode(spec.key(), NodeKind.BUTTON, button);
                rendered.buttonClick = onClick;
                button.setOnAction(event -> rendered.buttonClick.run());
                yield rendered;
            }
            case TextInputNode(String value, String placeholder, Consumer<String> onChange) -> {
                TextField textField = new TextField(value);
                textField.setPromptText(placeholder);
                RenderedNode rendered = new RenderedNode(spec.key(), NodeKind.INPUT, textField);
                rendered.inputChange = onChange;
                textField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!rendered.syncingInput && rendered.inputChange != null) {
                        rendered.inputChange.accept(newValue);
                    }
                });
                yield rendered;
            }
            case ContainerNode(List<JNode> children) -> {
                VBox vBox = new VBox(8);
                RenderedNode rendered = new RenderedNode(spec.key(), NodeKind.CONTAINER, vBox);
                List<RenderedNode> childNodes = new ArrayList<>(children.size());
                for (NodeSpec childSpec : normalizeChildren(children)) {
                    RenderedNode childRendered = createRenderedNode(childSpec);
                    childNodes.add(childRendered);
                    vBox.getChildren().add(childRendered.fxNode);
                }
                rendered.children = childNodes;
                yield rendered;
            }
            case ScrollAreaNode(JNode child) -> {
                NodeSpec childSpec = normalize(child);
                RenderedNode childRendered = createRenderedNode(childSpec);
                ScrollPane scrollPane = new ScrollPane(childRendered.fxNode);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(false);
                scrollPane.setPannable(true);
                scrollPane.setFocusTraversable(false);
                scrollPane.getStyleClass().add("edge-to-edge");

                RenderedNode rendered = new RenderedNode(spec.key(), NodeKind.SCROLL_AREA, scrollPane);
                rendered.children = List.of(childRendered);
                yield rendered;
            }
            default -> throw unsupportedNode(spec.node());
        };
    }

    private NodeSpec normalize(JNode node) {
        JNode current = node;
        String key = null;
        while (current instanceof KeyedNode keyedNode) {
            key = keyedNode.key();
            current = keyedNode.child();
        }
        return new NodeSpec(key, current);
    }

    private NodeKind kindOf(JNode node) {
        if (node instanceof TextNode) {
            return NodeKind.TEXT;
        }
        if (node instanceof ButtonNode) {
            return NodeKind.BUTTON;
        }
        if (node instanceof TextInputNode) {
            return NodeKind.INPUT;
        }
        if (node instanceof ContainerNode) {
            return NodeKind.CONTAINER;
        }
        if (node instanceof ScrollAreaNode) {
            return NodeKind.SCROLL_AREA;
        }
        throw unsupportedNode(node);
    }

    private JactRuntimeException unsupportedNode(JNode node) {
        return new JactRuntimeException("Unsupported JNode type: " + node.getClass().getName());
    }

    private enum NodeKind {
        TEXT,
        BUTTON,
        INPUT,
        CONTAINER,
        SCROLL_AREA
    }

    private record NodeSpec(String key, JNode node) {
    }

    private static final class RenderedNode {
        private String key;
        private final NodeKind kind;
        private final Node fxNode;
        private List<RenderedNode> children = List.of();
        private Runnable buttonClick;
        private Consumer<String> inputChange;
        private boolean syncingInput;

        private RenderedNode(String key, NodeKind kind, Node fxNode) {
            this.key = key;
            this.kind = kind;
            this.fxNode = fxNode;
        }
    }
}
