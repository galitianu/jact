package io.jact.sample;

import io.jact.annotations.JNode;
import io.jact.annotations.JactPage;
import io.jact.core.api.Hooks;
import io.jact.core.node.Nodes;
import io.jact.sample.tasks.TaskService;
import org.springframework.stereotype.Component;

@Component
public class HomePage {
    @JactPage(path = "/")
    public JNode home(TaskService taskService) {
        Hooks.useExternal(taskService.changes());

        return Nodes.column(
            Nodes.component("taskHeader"),
            Nodes.divider(),
            Nodes.component("taskFilters"),
            Nodes.component("createTaskForm"),
            Nodes.component("taskList")
        );
    }
}
