package com.lenaneshcheret.taskmanager.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must be at most 150 characters")
    String name
) {
}
