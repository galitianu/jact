package io.jact.baseline;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class TaskManagerController {
    private final InMemoryTaskService taskService;
    private final List<String> history = new ArrayList<>();

    @FXML
    private VBox listPage;
    @FXML
    private VBox detailPage;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<TaskFilter> filterBox;
    @FXML
    private CheckBox descendingBox;
    @FXML
    private TextField newTaskField;
    @FXML
    private Label taskListTitle;
    @FXML
    private VBox taskListContainer;
    @FXML
    private Label detailIdLabel;
    @FXML
    private TextField editTitleField;
    @FXML
    private CheckBox detailCompletedBox;
    @FXML
    private Label currentRouteLabel;

    private String currentPath = "/";
    private TaskView selectedTask;

    public TaskManagerController(InMemoryTaskService taskService) {
        this.taskService = taskService;
    }

    @FXML
    void initialize() {
        filterBox.setItems(FXCollections.observableArrayList(TaskFilter.values()));
        filterBox.setValue(TaskFilter.ALL);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshList());
        filterBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshList());
        descendingBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshList());
        showList();
        refreshList();
    }

    @FXML
    void createTask() {
        String title = newTaskField.getText();
        if (title != null && !title.trim().isBlank()) {
            taskService.create(title);
            newTaskField.clear();
            refreshList();
        }
    }

    @FXML
    void saveSelectedTask() {
        if (selectedTask == null) {
            return;
        }

        String newValue = editTitleField.getText() == null ? "" : editTitleField.getText().trim();
        if (!newValue.isEmpty()) {
            taskService.updateTitle(selectedTask.id(), newValue);
        }
        refreshCurrentDetail();
        refreshList();
    }

    @FXML
    void deleteSelectedTask() {
        if (selectedTask == null) {
            return;
        }

        taskService.delete(selectedTask.id());
        replace("/");
        refreshList();
    }

    @FXML
    void goBack() {
        if (history.isEmpty()) {
            replace("/");
            return;
        }

        String previous = history.removeLast();
        navigateTo(previous, false);
    }

    private void refreshList() {
        List<TaskView> tasks = taskService.list(
            searchField == null ? "" : searchField.getText(),
            filterBox == null ? TaskFilter.ALL : filterBox.getValue(),
            descendingBox != null && descendingBox.isSelected()
        );

        taskListTitle.setText("Task list (" + tasks.size() + ")");
        taskListContainer.getChildren().clear();

        if (tasks.isEmpty()) {
            taskListContainer.getChildren().add(new Label("No tasks match your current search/filter."));
            return;
        }

        for (TaskView task : tasks) {
            taskListContainer.getChildren().add(createTaskRow(task));
        }
    }

    private HBox createTaskRow(TaskView task) {
        CheckBox completed = new CheckBox("");
        completed.setSelected(task.completed());
        completed.selectedProperty().addListener((observable, oldValue, newValue) -> {
            taskService.toggleCompleted(task.id());
            refreshList();
            if (selectedTask != null && selectedTask.id() == task.id()) {
                refreshCurrentDetail();
            }
        });

        Label title = new Label("#" + task.id() + " " + task.title());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button open = new Button("Open");
        open.setOnAction(event -> push("/tasks/" + task.id()));

        Button delete = new Button("Delete");
        delete.setOnAction(event -> {
            taskService.delete(task.id());
            refreshList();
            if (selectedTask != null && selectedTask.id() == task.id()) {
                replace("/");
            }
        });

        HBox row = new HBox(10, completed, title, spacer, open, delete);
        row.getStyleClass().add("task-row");
        return row;
    }

    private void push(String path) {
        history.add(currentPath);
        navigateTo(path, true);
    }

    private void replace(String path) {
        navigateTo(path, false);
    }

    private void navigateTo(String path, boolean keepHistory) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            showList();
            return;
        }

        if (path.startsWith("/tasks/")) {
            String idParam = path.substring("/tasks/".length());
            try {
                long taskId = Long.parseLong(idParam);
                taskService.findById(taskId).ifPresentOrElse(
                    task -> showDetails(path, task),
                    () -> showMissingTask(path, taskId)
                );
            } catch (RuntimeException ignored) {
                showInvalidTask(path, idParam);
            }
            return;
        }

        if (keepHistory) {
            history.removeLast();
        }
        showList();
    }

    private void showList() {
        currentPath = "/";
        selectedTask = null;
        listPage.setVisible(true);
        listPage.setManaged(true);
        detailPage.setVisible(false);
        detailPage.setManaged(false);
    }

    private void showDetails(String path, TaskView task) {
        currentPath = path;
        selectedTask = task;
        listPage.setVisible(false);
        listPage.setManaged(false);
        detailPage.setVisible(true);
        detailPage.setManaged(true);
        detailIdLabel.setText("Task id: " + task.id());
        detailCompletedBox.setSelected(task.completed());
        detailCompletedBox.setOnAction(event -> {
            if (selectedTask != null) {
                taskService.toggleCompleted(selectedTask.id());
                refreshCurrentDetail();
                refreshList();
            }
        });
        editTitleField.setText(task.title());
        currentRouteLabel.setText("Current route: " + currentPath);
    }

    private void showMissingTask(String path, long taskId) {
        currentPath = path;
        selectedTask = null;
        listPage.setVisible(false);
        listPage.setManaged(false);
        detailPage.setVisible(true);
        detailPage.setManaged(true);
        detailIdLabel.setText("Task #" + taskId + " was not found.");
        detailCompletedBox.setSelected(false);
        editTitleField.clear();
        currentRouteLabel.setText("Current route: " + currentPath);
    }

    private void showInvalidTask(String path, String idParam) {
        currentPath = path;
        selectedTask = null;
        listPage.setVisible(false);
        listPage.setManaged(false);
        detailPage.setVisible(true);
        detailPage.setManaged(true);
        detailIdLabel.setText("Invalid task id: " + idParam);
        detailCompletedBox.setSelected(false);
        editTitleField.clear();
        currentRouteLabel.setText("Current route: " + currentPath);
    }

    private void refreshCurrentDetail() {
        if (selectedTask == null) {
            return;
        }
        taskService.findById(selectedTask.id()).ifPresentOrElse(
            task -> showDetails("/tasks/" + task.id(), task),
            () -> replace("/")
        );
    }
}
