package com.lenaneshcheret.taskmanager.controller.dto;

import com.lenaneshcheret.taskmanager.domain.ExportJobType;
import jakarta.validation.constraints.NotNull;

public record ExportRequest(
    @NotNull(message = "projectId is required")
    Long projectId,
    @NotNull(message = "type is required")
    ExportJobType type
) {
}
