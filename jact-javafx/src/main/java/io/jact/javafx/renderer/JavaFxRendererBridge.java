package io.jact.javafx.renderer;

import io.jact.annotations.JNode;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.node.ButtonNode;
import io.jact.core.node.CheckboxNode;
import io.jact.core.node.ContainerNode;
import io.jact.core.node.DividerNode;
import io.jact.core.node.KeyedNode;
import io.jact.core.node.RowNode;
import io.jact.core.node.ScrollAreaNode;
import io.jact.core.node.SelectNode;
import io.jact.core.node.NodeStyle;
import io.jact.core.node.SpacerNode;
import io.jact.core.node.StyledNode;
import io.jact.core.node.TextInputNode;
import io.jact.core.node.TextNode;
import javafx.collections.ObservableList;
import io.jact.core.runtime.WindowSettings;
import io.jact.core.runtime.spi.RendererBridge;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
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
        applyStyle(current, next.style());
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
            case CheckboxNode(String label, boolean checked, Consumer<Boolean> onChange) -> {
                CheckBox checkBox = (CheckBox) current.fxNode;
                if (!label.equals(checkBox.getText())) {
                    checkBox.setText(label);
                }
                current.checkboxChange = onChange;
                if (checkBox.isSelected() != checked) {
                    current.syncingCheckbox = true;
                    try {
                        checkBox.setSelected(checked);
                    } finally {
                        current.syncingCheckbox = false;
                    }
                }
                return current;
            }
            case SelectNode(String value, List<String> options, Consumer<String> onChange) -> {
                ComboBox<String> comboBox = typedComboBox(current.fxNode);
                current.selectChange = onChange;
                if (!comboBox.getItems().equals(options)) {
                    comboBox.getItems().setAll(options);
                }
                if (!value.equals(comboBox.getValue())) {
                    current.syncingSelect = true;
                    try {
                        comboBox.setValue(value);
                    } finally {
                        current.syncingSelect = false;
                    }
                }
                return current;
            }
            case ContainerNode(List<JNode> children) -> {
                reconcileLinearContainer(current, children, ((VBox) current.fxNode).getChildren());
                return current;
            }
            case RowNode(List<JNode> children) -> {
                reconcileLinearContainer(current, children, ((HBox) current.fxNode).getChildren());
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
            case SpacerNode ignored -> {
                return current;
            }
            case DividerNode ignored -> {
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

    private void reconcileLinearContainer(RenderedNode containerHolder, List<JNode> nextChildrenRaw, ObservableList<Node> fxChildren) {
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
        if (!sameOrder(fxChildren, expectedOrder)) {
            fxChildren.setAll(expectedOrder);
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
        RenderedNode rendered = switch (spec.node()) {
            case TextNode(String value) -> new RenderedNode(spec.key(), NodeKind.TEXT, new Label(value));
            case ButtonNode(String label, Runnable onClick) -> {
                Button button = new Button(label);
                RenderedNode created = new RenderedNode(spec.key(), NodeKind.BUTTON, button);
                created.buttonClick = onClick;
                button.setOnAction(event -> created.buttonClick.run());
                yield created;
            }
            case TextInputNode(String value, String placeholder, Consumer<String> onChange) -> {
                TextField textField = new TextField(value);
                textField.setPromptText(placeholder);
                RenderedNode created = new RenderedNode(spec.key(), NodeKind.INPUT, textField);
                created.inputChange = onChange;
                textField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!created.syncingInput && created.inputChange != null) {
                        created.inputChange.accept(newValue);
                    }
                });
                yield created;
            }
            case CheckboxNode(String label, boolean checked, Consumer<Boolean> onChange) -> {
                CheckBox checkBox = new CheckBox(label);
                checkBox.setSelected(checked);
                RenderedNode created = new RenderedNode(spec.key(), NodeKind.CHECKBOX, checkBox);
                created.checkboxChange = onChange;
                checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (!created.syncingCheckbox && created.checkboxChange != null) {
                        created.checkboxChange.accept(newValue);
                    }
                });
                yield created;
            }
            case SelectNode(String value, List<String> options, Consumer<String> onChange) -> {
                ComboBox<String> comboBox = new ComboBox<>();
                comboBox.getItems().setAll(options);
                comboBox.setValue(value);
                RenderedNode created = new RenderedNode(spec.key(), NodeKind.SELECT, comboBox);
                created.selectChange = onChange;
                comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                    if (!created.syncingSelect && created.selectChange != null) {
                        created.selectChange.accept(newValue);
                    }
                });
                yield created;
            }
            case ContainerNode(List<JNode> children) -> {
                VBox vBox = new VBox(8);
                RenderedNode created = new RenderedNode(spec.key(), NodeKind.CONTAINER, vBox);
                List<RenderedNode> childNodes = new ArrayList<>(children.size());
                for (NodeSpec childSpec : normalizeChildren(children)) {
                    RenderedNode childRendered = createRenderedNode(childSpec);
                    childNodes.add(childRendered);
                    vBox.getChildren().add(childRendered.fxNode);
                }
                created.children = childNodes;
                yield created;
            }
            case RowNode(List<JNode> children) -> {
                HBox hBox = new HBox(8);
                RenderedNode created = new RenderedNode(spec.key(), NodeKind.ROW, hBox);
                List<RenderedNode> childNodes = new ArrayList<>(children.size());
                for (NodeSpec childSpec : normalizeChildren(children)) {
                    RenderedNode childRendered = createRenderedNode(childSpec);
                    childNodes.add(childRendered);
                    hBox.getChildren().add(childRendered.fxNode);
                }
                created.children = childNodes;
                yield created;
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

                RenderedNode created = new RenderedNode(spec.key(), NodeKind.SCROLL_AREA, scrollPane);
                created.children = List.of(childRendered);
                yield created;
            }
            case SpacerNode ignored -> {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                VBox.setVgrow(spacer, Priority.ALWAYS);
                yield new RenderedNode(spec.key(), NodeKind.SPACER, spacer);
            }
            case DividerNode ignored -> new RenderedNode(spec.key(), NodeKind.DIVIDER, new Separator());
            default -> throw unsupportedNode(spec.node());
        };
        applyStyle(rendered, spec.style());
        return rendered;
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> typedComboBox(Node node) {
        return (ComboBox<String>) node;
    }

    private NodeSpec normalize(JNode node) {
        JNode current = node;
        String key = null;
        NodeStyle style = NodeStyle.empty();
        while (current instanceof KeyedNode keyedNode) {
            key = keyedNode.key();
            current = keyedNode.child();
        }
        while (current instanceof StyledNode styledNode || current instanceof KeyedNode) {
            if (current instanceof StyledNode styledNode) {
                style = style.merge(styledNode.style());
                current = styledNode.child();
            } else if (current instanceof KeyedNode keyedNode) {
                key = keyedNode.key();
                current = keyedNode.child();
            }
        }
        return new NodeSpec(key, current, style);
    }

    private void applyStyle(RenderedNode rendered, NodeStyle style) {
        Node node = rendered.fxNode;
        node.getStyleClass().removeAll(rendered.appliedStyleClasses);
        rendered.appliedStyleClasses = style.styleClasses();
        node.getStyleClass().addAll(rendered.appliedStyleClasses);
        node.setStyle(style.inlineStyle());

        if (style.disabled() != null) {
            node.setDisable(style.disabled());
        }

        if (node instanceof Region region) {
            if (style.minWidth() != null) {
                region.setMinWidth(style.minWidth());
            }
            if (style.prefWidth() != null) {
                region.setPrefWidth(style.prefWidth());
            }
            if (style.maxWidth() != null) {
                region.setMaxWidth(style.maxWidth());
            }
            if (style.minHeight() != null) {
                region.setMinHeight(style.minHeight());
            }
            if (style.prefHeight() != null) {
                region.setPrefHeight(style.prefHeight());
            }
            if (style.maxHeight() != null) {
                region.setMaxHeight(style.maxHeight());
            }
        }

        if (style.spacing() != null) {
            if (node instanceof VBox vBox) {
                vBox.setSpacing(style.spacing());
            } else if (node instanceof HBox hBox) {
                hBox.setSpacing(style.spacing());
            }
        }

        if (style.alignment() != null) {
            Pos alignment = Pos.valueOf(style.alignment().trim().toUpperCase().replace('-', '_'));
            if (node instanceof VBox vBox) {
                vBox.setAlignment(alignment);
            } else if (node instanceof HBox hBox) {
                hBox.setAlignment(alignment);
            }
        }
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
        if (node instanceof CheckboxNode) {
            return NodeKind.CHECKBOX;
        }
        if (node instanceof SelectNode) {
            return NodeKind.SELECT;
        }
        if (node instanceof ContainerNode) {
            return NodeKind.CONTAINER;
        }
        if (node instanceof RowNode) {
            return NodeKind.ROW;
        }
        if (node instanceof ScrollAreaNode) {
            return NodeKind.SCROLL_AREA;
        }
        if (node instanceof SpacerNode) {
            return NodeKind.SPACER;
        }
        if (node instanceof DividerNode) {
            return NodeKind.DIVIDER;
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
        CHECKBOX,
        SELECT,
        CONTAINER,
        ROW,
        SCROLL_AREA,
        SPACER,
        DIVIDER
    }

    private record NodeSpec(String key, JNode node, NodeStyle style) {
    }

    private static final class RenderedNode {
        private String key;
        private final NodeKind kind;
        private final Node fxNode;
        private List<RenderedNode> children = List.of();
        private Runnable buttonClick;
        private Consumer<String> inputChange;
        private Consumer<Boolean> checkboxChange;
        private Consumer<String> selectChange;
        private boolean syncingInput;
        private boolean syncingCheckbox;
        private boolean syncingSelect;
        private List<String> appliedStyleClasses = List.of();

        private RenderedNode(String key, NodeKind kind, Node fxNode) {
            this.key = key;
            this.kind = kind;
            this.fxNode = fxNode;
        }
    }
}
