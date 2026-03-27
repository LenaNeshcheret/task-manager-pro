package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.domain.ExportJobStatus;
import com.lenaneshcheret.taskmanager.domain.ExportJobType;
import com.lenaneshcheret.taskmanager.repository.ExportJobRepository;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportJobLifecycleService {

  private static final int MAX_ERROR_LENGTH = 1_000;

  private final ExportJobRepository exportJobRepository;
  private final Clock clock;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<ExportJobExecution> start(Long jobId) {
    return exportJobRepository.findByIdForUpdate(jobId)
        .filter(job -> job.getStatus() == ExportJobStatus.PENDING)
        .map(job -> {
          job.markRunning();
          return new ExportJobExecution(job.getId(), job.getProjectId(), job.getType());
        });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(Long jobId, String filePath) {
    exportJobRepository.findByIdForUpdate(jobId)
        .ifPresent(job -> job.markDone(filePath, clock.instant()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void fail(Long jobId, String errorMessage) {
    exportJobRepository.findByIdForUpdate(jobId)
        .ifPresent(job -> job.markFailed(truncate(errorMessage), clock.instant()));
  }

  public record ExportJobExecution(Long jobId, Long projectId, ExportJobType type) {
  }

  private String truncate(String errorMessage) {
    if (errorMessage == null || errorMessage.isBlank()) {
      return "Export generation failed";
    }
    return errorMessage.length() <= MAX_ERROR_LENGTH
        ? errorMessage
        : errorMessage.substring(0, MAX_ERROR_LENGTH);
  }
}
