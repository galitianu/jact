package io.jact.core.node;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScrollAreaNodeTest {
    @Test
    void createsScrollAreaNodeThroughFactory() {
        TextNode child = new TextNode("value");

        ScrollAreaNode scrollAreaNode = Nodes.scrollArea(child);

        assertThat(scrollAreaNode.child()).isSameAs(child);
    }

    @Test
    void rejectsNullChild() {
        assertThatThrownBy(() -> Nodes.scrollArea(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("child");
    }
}
