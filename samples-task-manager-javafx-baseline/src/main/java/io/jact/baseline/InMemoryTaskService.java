package io.jact.baseline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTaskService {
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, TaskView> tasks = new LinkedHashMap<>();

    public synchronized TaskView create(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }
        TaskView task = new TaskView(ids.incrementAndGet(), normalized, false);
        tasks.put(task.id(), task);
        return task;
    }

    public synchronized List<TaskView> list(String searchQuery, TaskFilter filter, boolean descending) {
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase();
        TaskFilter effectiveFilter = filter == null ? TaskFilter.ALL : filter;

        Comparator<TaskView> comparator = Comparator.comparingLong(TaskView::id);
        if (descending) {
            comparator = comparator.reversed();
        }

        return tasks.values().stream()
            .filter(task -> query.isBlank() || task.title().toLowerCase().contains(query))
            .filter(task -> effectiveFilter == TaskFilter.ALL
                || effectiveFilter == TaskFilter.DONE && task.completed()
                || effectiveFilter == TaskFilter.OPEN && !task.completed())
            .sorted(comparator)
            .toList();
    }

    public synchronized Optional<TaskView> findById(long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public synchronized void updateTitle(long id, String title) {
        TaskView current = requireTask(id);
        String normalized = title == null ? "" : title.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }
        tasks.put(id, new TaskView(current.id(), normalized, current.completed()));
    }

    public synchronized void toggleCompleted(long id) {
        TaskView current = requireTask(id);
        tasks.put(id, new TaskView(current.id(), current.title(), !current.completed()));
    }

    public synchronized void delete(long id) {
        tasks.remove(id);
    }

    public synchronized List<TaskView> snapshot() {
        return new ArrayList<>(tasks.values());
    }

    private TaskView requireTask(long id) {
        TaskView task = tasks.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + id);
        }
        return task;
    }
}
