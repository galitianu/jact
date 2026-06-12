package io.jact.sample.pages.tasks.$id;

import io.jact.annotations.JNode;
import io.jact.annotations.JactPage;
import io.jact.core.api.Hooks;
import io.jact.core.api.Navigator;
import io.jact.core.node.Nodes;
import io.jact.core.routing.RouteParams;
import io.jact.sample.tasks.TaskService;
import io.jact.sample.tasks.TaskView;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TaskDetailPage {
    @JactPage(path = "/tasks/$id")
    public JNode taskDetail(RouteParams params, Navigator navigator, TaskService taskService) {
        Hooks.useExternal(taskService.changes());

        String idParam = params.get("id");
        long taskId;
        try {
            taskId = Long.parseLong(idParam);
        } catch (RuntimeException ignored) {
            return Nodes.column(
                Nodes.text("Task details"),
                Nodes.text("Invalid task id: " + idParam),
                Nodes.button("Back", navigator::back)
            );
        }

        Optional<TaskView> taskOptional = taskService.findById(taskId);
        if (taskOptional.isEmpty()) {
            return Nodes.column(
                Nodes.text("Task details"),
                Nodes.text("Task #" + taskId + " was not found."),
                Nodes.button("Back", navigator::back)
            );
        }

        TaskView task = taskOptional.get();
        return Nodes.component("taskDetailForm", task);
    }
}
