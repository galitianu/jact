package io.jact.baseline;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TaskManagerBaselineApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(TaskManagerBaselineApplication.class.getResource("/io/jact/baseline/task-manager.fxml"));
        loader.setControllerFactory(type -> new TaskManagerController(new InMemoryTaskService()));
        stage.setTitle("JACT M3 Task Manager");
        stage.setScene(new Scene(loader.load(), 960, 640));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
