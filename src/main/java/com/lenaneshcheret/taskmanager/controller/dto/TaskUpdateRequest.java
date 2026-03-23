package com.lenaneshcheret.taskmanager.controller.dto;

import com.lenaneshcheret.taskmanager.domain.TaskPriority;
import com.lenaneshcheret.taskmanager.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record TaskUpdateRequest(
    @NotNull(message = "version is required")
    Long version,
    @Size(max = 200, message = "title must be at most 200 characters")
    String title,
    String description,
    TaskStatus status,
    Instant dueAt,
    TaskPriority priority
) {
}
