package io.jact.baseline;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class TaskManagerController {
    private final InMemoryTaskService taskService;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<TaskFilter> filterBox;
    @FXML
    private CheckBox descendingBox;
    @FXML
    private TextField newTaskField;
    @FXML
    private ListView<TaskView> taskList;
    @FXML
    private VBox detailPane;
    @FXML
    private Label detailTitle;
    @FXML
    private TextField editTitleField;
    @FXML
    private CheckBox detailCompletedBox;

    private TaskView selectedTask;

    public TaskManagerController(InMemoryTaskService taskService) {
        this.taskService = taskService;
    }

    @FXML
    void initialize() {
        filterBox.setItems(FXCollections.observableArrayList(TaskFilter.values()));
        filterBox.setValue(TaskFilter.ALL);
        taskList.setCellFactory(ignored -> new TaskListCell());
        taskList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> showDetails(newValue));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshList());
        filterBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshList());
        descendingBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshList());
        detailPane.setDisable(true);
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
        taskService.updateTitle(selectedTask.id(), editTitleField.getText());
        if (detailCompletedBox.isSelected() != selectedTask.completed()) {
            taskService.toggleCompleted(selectedTask.id());
        }
        refreshList();
        taskService.findById(selectedTask.id()).ifPresent(this::showDetails);
    }

    @FXML
    void deleteSelectedTask() {
        if (selectedTask == null) {
            return;
        }
        taskService.delete(selectedTask.id());
        showDetails(null);
        refreshList();
    }

    private void refreshList() {
        taskList.getItems().setAll(taskService.list(
            searchField == null ? "" : searchField.getText(),
            filterBox == null ? TaskFilter.ALL : filterBox.getValue(),
            descendingBox != null && descendingBox.isSelected()
        ));
    }

    private void showDetails(TaskView task) {
        selectedTask = task;
        boolean disabled = task == null;
        detailPane.setDisable(disabled);
        if (disabled) {
            detailTitle.setText("No task selected");
            editTitleField.clear();
            detailCompletedBox.setSelected(false);
            return;
        }
        detailTitle.setText("Task #" + task.id());
        editTitleField.setText(task.title());
        detailCompletedBox.setSelected(task.completed());
    }

    private final class TaskListCell extends ListCell<TaskView> {
        @Override
        protected void updateItem(TaskView item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Button toggle = new Button(item.completed() ? "Mark Open" : "Mark Done");
            toggle.setOnAction(event -> {
                taskService.toggleCompleted(item.id());
                refreshList();
            });
            setText("#" + item.id() + " [" + (item.completed() ? "DONE" : "OPEN") + "] " + item.title());
            setGraphic(toggle);
        }
    }
}
