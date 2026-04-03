package io.jact.sample.tasks;

public record TaskView(
    Long id,
    String title,
    boolean completed
) {
}
