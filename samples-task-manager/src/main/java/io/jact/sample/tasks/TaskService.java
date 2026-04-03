package io.jact.sample.tasks;

import io.jact.core.api.ObservableValue;
import io.jact.spring.boot.bridge.ServiceStateBridge;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final ServiceStateBridge<Long> revision = new ServiceStateBridge<>(0L);

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @PostConstruct
    void seedDefaults() {
        if (taskRepository.count() == 0) {
            taskRepository.save(new TaskEntity("Prepare M3 validation notes"));
            taskRepository.save(new TaskEntity("Review service-driven rerender flow"));
        }
    }

    public List<TaskView> list(String query, TaskFilter filter, boolean sortDescending) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        TaskFilter normalizedFilter = filter == null ? TaskFilter.ALL : filter;

        Comparator<TaskEntity> comparator = Comparator.comparing(TaskEntity::getId);
        if (sortDescending) {
            comparator = comparator.reversed();
        }

        return taskRepository.findAll().stream()
            .filter(task -> normalizedQuery.isBlank() || task.getTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery))
            .filter(task -> matchesFilter(task, normalizedFilter))
            .sorted(comparator)
            .map(task -> new TaskView(task.getId(), task.getTitle(), task.isCompleted()))
            .toList();
    }

    public Optional<TaskView> findById(long id) {
        return taskRepository.findById(id)
            .map(task -> new TaskView(task.getId(), task.getTitle(), task.isCompleted()));
    }

    public TaskView create(String title) {
        String normalized = normalizeTitle(title);
        TaskEntity saved = taskRepository.save(new TaskEntity(normalized));
        bumpRevision();
        return new TaskView(saved.getId(), saved.getTitle(), saved.isCompleted());
    }

    public void updateTitle(long id, String newTitle) {
        String normalized = normalizeTitle(newTitle);
        Optional<TaskView> result = taskRepository.findById(id)
            .map(task -> {
                task.setTitle(normalized);
                TaskEntity saved = taskRepository.save(task);
                return new TaskView(saved.getId(), saved.getTitle(), saved.isCompleted());
            });

        result.ifPresent(ignored -> bumpRevision());
    }

    public void toggleCompleted(long id) {
        Optional<TaskEntity> taskOptional = taskRepository.findById(id);
        if (taskOptional.isEmpty()) {
            return;
        }

        TaskEntity task = taskOptional.get();
        task.setCompleted(!task.isCompleted());
        taskRepository.save(task);
        bumpRevision();
    }

    public void delete(long id) {
        if (!taskRepository.existsById(id)) {
            return;
        }
        taskRepository.deleteById(id);
        bumpRevision();
    }

    public ObservableValue<Long> changes() {
        return revision;
    }

    private boolean matchesFilter(TaskEntity task, TaskFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case OPEN -> !task.isCompleted();
            case DONE -> task.isCompleted();
        };
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Task title must not be blank.");
        }
        return normalized;
    }

    private void bumpRevision() {
        revision.update(current -> current + 1);
    }
}
