package io.jact.sample.tasks;

import io.jact.core.api.SimpleStore;
import io.jact.core.api.Store;
import org.springframework.stereotype.Component;

@Component
public class TaskUiState {
    private final Store<String> searchQuery = new SimpleStore<>("");
    private final Store<TaskFilter> filter = new SimpleStore<>(TaskFilter.ALL);
    private final Store<Boolean> sortDescending = new SimpleStore<>(false);

    public Store<String> searchQuery() {
        return searchQuery;
    }

    public Store<TaskFilter> filter() {
        return filter;
    }

    public Store<Boolean> sortDescending() {
        return sortDescending;
    }
}
