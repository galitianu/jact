package io.jact.sample.tasks;

import io.jact.annotations.JNode;
import io.jact.annotations.JactComponent;
import io.jact.core.api.Hooks;
import io.jact.core.api.Navigator;
import io.jact.core.api.State;
import io.jact.core.node.NodeStyle;
import io.jact.core.node.Nodes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TaskComponents {
    private static final List<String> FILTER_OPTIONS = List.of("ALL", "OPEN", "DONE");

    @JactComponent
    public JNode taskHeader() {
        return Nodes.style(
            Nodes.column(
                Nodes.text("JACT Thesis Task Manager"),
                Nodes.text("Reactive Java desktop UI powered by Spring Boot services")
            ),
            NodeStyle.empty().withClassName("task-header").withSpacing(4)
        );
    }

    @JactComponent
    public JNode taskFilters(TaskUiState uiState) {
        String searchQuery = Hooks.useStore(uiState.searchQuery());
        TaskFilter filter = Hooks.useStore(uiState.filter());
        Boolean sortDescending = Hooks.useStore(uiState.sortDescending());

        return Nodes.style(
            Nodes.column(
                Nodes.text("Search and filters"),
                Nodes.input(searchQuery, "Search tasks by title", uiState.searchQuery()::set),
                Nodes.row(
                    Nodes.select(filter.name(), FILTER_OPTIONS, value -> uiState.filter().set(TaskFilter.valueOf(value))),
                    Nodes.checkbox("Latest first", sortDescending, uiState.sortDescending()::set)
                )
            ),
            NodeStyle.empty().withClassName("task-filters").withSpacing(8)
        );
    }

    @JactComponent
    public JNode createTaskForm(TaskService taskService) {
        State<String> newTaskTitle = Hooks.useState("");

        return Nodes.style(
            Nodes.column(
                Nodes.text("Create task"),
                Nodes.row(
                    Nodes.input(newTaskTitle.get(), "Task title", newTaskTitle::set),
                    Nodes.button("Create", () -> {
                        String title = newTaskTitle.get().trim();
                        if (!title.isEmpty()) {
                            taskService.create(title);
                            newTaskTitle.set("");
                        }
                    })
                )
            ),
            NodeStyle.empty().withClassName("create-task").withSpacing(8)
        );
    }

    @JactComponent
    public JNode taskList(TaskService taskService, TaskUiState uiState, Navigator navigator) {
        String searchQuery = Hooks.useStore(uiState.searchQuery());
        TaskFilter filter = Hooks.useStore(uiState.filter());
        Boolean sortDescending = Hooks.useStore(uiState.sortDescending());
        List<TaskView> tasks = taskService.list(searchQuery, filter, sortDescending);

        List<JNode> taskListNodes = new ArrayList<>();
        if (tasks.isEmpty()) {
            taskListNodes.add(Nodes.text("No tasks match your current search/filter."));
        } else {
            for (TaskView task : tasks) {
                taskListNodes.add(Nodes.key("task-row-" + task.id(), Nodes.component("taskRow", task)));
            }
        }

        return Nodes.column(
            Nodes.text("Task list (" + tasks.size() + ")"),
            Nodes.scrollArea(Nodes.column(taskListNodes.toArray(JNode[]::new)))
        );
    }

    @JactComponent
    public JNode taskRow(TaskService taskService, Navigator navigator, TaskView task) {
        return Nodes.style(
            Nodes.row(
                Nodes.checkbox("", task.completed(), checked -> taskService.toggleCompleted(task.id())),
                Nodes.text("#" + task.id() + " " + task.title()),
                Nodes.spacer(),
                Nodes.button("Open", () -> navigator.push("/tasks/" + task.id())),
                Nodes.button("Delete", () -> taskService.delete(task.id()))
            ),
            NodeStyle.empty().withClassName("task-row").withSpacing(10)
        );
    }

    @JactComponent
    public JNode taskDetailForm(TaskService taskService, Navigator navigator, TaskView task) {
        State<String> editTitle = Hooks.useState(task.title());

        return Nodes.style(
            Nodes.column(
                Nodes.text("Task details"),
                Nodes.text("Task id: " + task.id()),
                Nodes.checkbox("Completed", task.completed(), checked -> taskService.toggleCompleted(task.id())),
                Nodes.input(editTitle.get(), "Edit title", editTitle::set),
                Nodes.row(
                    Nodes.button("Save title", () -> {
                        String newValue = editTitle.get().trim();
                        if (!newValue.isEmpty()) {
                            taskService.updateTitle(task.id(), newValue);
                        }
                    }),
                    Nodes.button("Delete task", () -> {
                        taskService.delete(task.id());
                        navigator.replace("/");
                    }),
                    Nodes.button("Back", () -> {
                        if (!navigator.back()) {
                            navigator.replace("/");
                        }
                    })
                ),
                Nodes.text("Current route: " + navigator.currentPath())
            ),
            NodeStyle.empty().withClassName("task-detail").withSpacing(8)
        );
    }
}
