package com.lenaneshcheret.taskmanager.controller.dto;

import com.lenaneshcheret.taskmanager.domain.ExportJob;
import com.lenaneshcheret.taskmanager.domain.ExportJobStatus;
import com.lenaneshcheret.taskmanager.domain.ExportJobType;
import java.time.Instant;

public record ExportJobResponse(
    Long jobId,
    Long projectId,
    ExportJobType type,
    ExportJobStatus status,
    Instant createdAt,
    Instant finishedAt,
    String errorMessage
) {

  public static ExportJobResponse from(ExportJob exportJob) {
    return new ExportJobResponse(
        exportJob.getId(),
        exportJob.getProjectId(),
        exportJob.getType(),
        exportJob.getStatus(),
        exportJob.getCreatedAt(),
        exportJob.getFinishedAt(),
        exportJob.getErrorMessage()
    );
  }
}
