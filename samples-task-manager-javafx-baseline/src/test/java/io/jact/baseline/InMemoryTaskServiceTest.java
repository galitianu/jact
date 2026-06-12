package io.jact.baseline;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTaskServiceTest {
    @Test
    void supportsCrudSearchFilterAndSorting() {
        InMemoryTaskService service = new InMemoryTaskService();
        TaskView first = service.create("Write baseline");
        TaskView second = service.create("Write JACT sample");

        service.toggleCompleted(second.id());

        assertThat(service.list("write", TaskFilter.ALL, false)).extracting(TaskView::id)
            .containsExactly(first.id(), second.id());
        assertThat(service.list("", TaskFilter.DONE, false)).extracting(TaskView::id)
            .containsExactly(second.id());
        assertThat(service.list("", TaskFilter.ALL, true)).extracting(TaskView::id)
            .containsExactly(second.id(), first.id());

        service.updateTitle(first.id(), "Updated baseline");
        assertThat(service.findById(first.id())).get().extracting(TaskView::title)
            .isEqualTo("Updated baseline");

        service.delete(first.id());
        assertThat(service.findById(first.id())).isEmpty();
    }
}
