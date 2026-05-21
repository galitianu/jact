package io.jact.core.node;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyedNodeTest {
    @Test
    void createsKeyedNodeThroughFactory() {
        TextNode child = new TextNode("value");
        KeyedNode keyedNode = Nodes.key("task-row-1", child);

        assertThat(keyedNode.key()).isEqualTo("task-row-1");
        assertThat(keyedNode.child()).isSameAs(child);
    }

    @Test
    void rejectsBlankKey() {
        assertThatThrownBy(() -> Nodes.key("   ", new TextNode("value")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("key must not be blank");
    }
}
