package com.lenaneshcheret.taskmanager.controller.dto;

import com.lenaneshcheret.taskmanager.domain.Task;
import com.lenaneshcheret.taskmanager.domain.TaskPriority;
import com.lenaneshcheret.taskmanager.domain.TaskStatus;
import java.time.Instant;

public record TaskResponse(
    Long id,
    Long projectId,
    String title,
    String description,
    TaskStatus status,
    Instant dueAt,
    Instant completedAt,
    TaskPriority priority,
    Long version
) {

  public static TaskResponse from(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getProjectId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getDueAt(),
        task.getCompletedAt(),
        task.getPriority(),
        task.getVersion()
    );
  }
}
