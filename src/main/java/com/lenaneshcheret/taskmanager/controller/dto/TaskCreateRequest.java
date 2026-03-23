package com.lenaneshcheret.taskmanager.controller.dto;

import com.lenaneshcheret.taskmanager.domain.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record TaskCreateRequest(
    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be at most 200 characters")
    String title,
    String description,
    Instant dueAt,
    TaskPriority priority
) {
}
