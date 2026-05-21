package io.jact.sample;

import io.jact.annotations.JNode;
import io.jact.annotations.JactPage;
import io.jact.core.api.Hooks;
import io.jact.core.api.Navigator;
import io.jact.core.api.State;
import io.jact.core.node.Nodes;
import io.jact.sample.tasks.TaskFilter;
import io.jact.sample.tasks.TaskService;
import io.jact.sample.tasks.TaskUiState;
import io.jact.sample.tasks.TaskView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HomePages {
    private final TaskUiState uiState;

    public HomePages(TaskUiState uiState) {
        this.uiState = uiState;
    }

    @JactPage(path = "/")
    public JNode home(TaskService taskService) {
        Hooks.useExternal(taskService.changes());

        String searchQuery = Hooks.useStore(uiState.searchQuery());
        TaskFilter filter = Hooks.useStore(uiState.filter());
        Boolean sortDescending = Hooks.useStore(uiState.sortDescending());
        Navigator navigator = Hooks.navigator();
        State<String> newTaskTitle = Hooks.useState("");

        List<TaskView> tasks = taskService.list(searchQuery, filter, sortDescending);
        List<JNode> nodes = new ArrayList<>();

        nodes.add(Nodes.text("JACT M3 Task Manager"));
        nodes.add(Nodes.text("Search"));
        nodes.add(Nodes.input(searchQuery, "Search tasks by title", uiState.searchQuery()::set));
        nodes.add(Nodes.text("Filter: " + filter));
        nodes.add(Nodes.button("Filter ALL", () -> uiState.filter().set(TaskFilter.ALL)));
        nodes.add(Nodes.button("Filter OPEN", () -> uiState.filter().set(TaskFilter.OPEN)));
        nodes.add(Nodes.button("Filter DONE", () -> uiState.filter().set(TaskFilter.DONE)));
        nodes.add(Nodes.button(sortDescending ? "Sort: Latest first" : "Sort: Oldest first", () -> uiState.sortDescending().set(!sortDescending)));

        nodes.add(Nodes.text("Create Task"));
        nodes.add(Nodes.input(newTaskTitle.get(), "Task title", newTaskTitle::set));
        nodes.add(Nodes.button("Create", () -> {
            String title = newTaskTitle.get().trim();
            if (!title.isEmpty()) {
                taskService.create(title);
                newTaskTitle.set("");
            }
        }));

        nodes.add(Nodes.text("Task list (" + tasks.size() + ")"));
        List<JNode> taskListNodes = new ArrayList<>();
        if (tasks.isEmpty()) {
            taskListNodes.add(Nodes.text("No tasks match your current search/filter."));
        } else {
            for (TaskView task : tasks) {
                String status = task.completed() ? "DONE" : "OPEN";
                taskListNodes.add(Nodes.key("task-row-" + task.id(), Nodes.column(
                    Nodes.text("#" + task.id() + " [" + status + "] " + task.title()),
                    Nodes.button("Open #" + task.id(), () -> navigator.push("/tasks/" + task.id())),
                    Nodes.button(task.completed() ? "Mark Open #" + task.id() : "Mark Done #" + task.id(), () -> taskService.toggleCompleted(task.id())),
                    Nodes.button("Delete #" + task.id(), () -> taskService.delete(task.id()))
                )));
            }
        }
        nodes.add(Nodes.scrollArea(Nodes.column(taskListNodes.toArray(JNode[]::new))));

        return Nodes.column(nodes.toArray(JNode[]::new));
    }
}
