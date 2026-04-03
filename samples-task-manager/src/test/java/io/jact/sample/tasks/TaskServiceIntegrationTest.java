package io.jact.sample.tasks;

import io.jact.core.api.Subscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "jact.enabled=false")
class TaskServiceIntegrationTest {
    @Autowired
    private TaskService taskService;

    @Test
    void supportsCrudAndSearchFilterFlows() {
        TaskView created = taskService.create("M3 integration test task");
        assertThat(created.id()).isNotNull();

        List<TaskView> queryResults = taskService.list("integration", TaskFilter.ALL, false);
        assertThat(queryResults).extracting(TaskView::id).contains(created.id());

        taskService.updateTitle(created.id(), "M3 updated task");
        List<TaskView> updatedResults = taskService.list("updated", TaskFilter.ALL, false);
        assertThat(updatedResults).extracting(TaskView::id).contains(created.id());

        taskService.toggleCompleted(created.id());
        List<TaskView> doneResults = taskService.list("", TaskFilter.DONE, false);
        assertThat(doneResults).extracting(TaskView::id).contains(created.id());

        taskService.delete(created.id());
        assertThat(taskService.findById(created.id())).isEmpty();
    }

    @Test
    void notifiesObserversWhenServiceStateChanges() {
        AtomicInteger notifications = new AtomicInteger();
        Subscription subscription = taskService.changes().subscribe(notifications::incrementAndGet);
        try {
            TaskView created = taskService.create("Observable test task");
            taskService.updateTitle(created.id(), "Observable test task updated");
            taskService.toggleCompleted(created.id());
            taskService.delete(created.id());
        } finally {
            subscription.unsubscribe();
        }

        assertThat(notifications.get()).isGreaterThanOrEqualTo(4);
    }
}
