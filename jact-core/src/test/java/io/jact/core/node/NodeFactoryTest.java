package io.jact.core.node;

import io.jact.annotations.JNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class NodeFactoryTest {
    @Test
    void createsInteractiveFormNodes() {
        AtomicBoolean checked = new AtomicBoolean();
        AtomicReference<String> selected = new AtomicReference<>();

        CheckboxNode checkbox = Nodes.checkbox("Done", true, checked::set);
        SelectNode select = Nodes.select("OPEN", List.of("OPEN", "DONE"), selected::set);

        checkbox.onChange().accept(false);
        select.onChange().accept("DONE");

        assertThat(checkbox.label()).isEqualTo("Done");
        assertThat(checkbox.checked()).isTrue();
        assertThat(checked.get()).isFalse();
        assertThat(select.options()).containsExactly("OPEN", "DONE");
        assertThat(selected.get()).isEqualTo("DONE");
    }

    @Test
    void createsLayoutNodes() {
        JNode first = Nodes.text("first");
        JNode second = Nodes.text("second");

        RowNode row = Nodes.row(first, second);
        SpacerNode spacer = Nodes.spacer();
        DividerNode divider = Nodes.divider();

        assertThat(row.children()).containsExactly(first, second);
        assertThat(spacer).isNotNull();
        assertThat(divider).isNotNull();
    }

    @Test
    void createsStyledNodes() {
        StyledNode styled = Nodes.className(
            "task-row",
            Nodes.style(
                Nodes.text("Task"),
                NodeStyle.empty()
                    .withClassName("primary")
                    .withInlineStyle("-fx-font-weight: bold;")
                    .withSpacing(12)
                    .withDisabled(false)
            )
        );

        assertThat(styled.style().styleClasses()).containsExactly("task-row");
        assertThat(((StyledNode) styled.child()).style().styleClasses()).containsExactly("primary");
        assertThat(((StyledNode) styled.child()).style().inlineStyle()).contains("bold");
    }
}
