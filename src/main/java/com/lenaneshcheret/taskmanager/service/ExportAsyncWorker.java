package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.domain.ExportJobType;
import com.lenaneshcheret.taskmanager.domain.Task;
import com.lenaneshcheret.taskmanager.repository.TaskRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportAsyncWorker {

  private final ExportJobLifecycleService exportJobLifecycleService;
  private final TaskRepository taskRepository;
  private final CsvExportWriter csvExportWriter;

  @Async
  public void process(Long jobId) {
    exportJobLifecycleService.start(jobId)
        .ifPresent(this::runExport);
  }

  private void runExport(ExportJobLifecycleService.ExportJobExecution execution) {
    try {
      Path filePath = generateFile(execution);
      exportJobLifecycleService.complete(execution.jobId(), filePath.toString());
    } catch (RuntimeException | IOException exception) {
      exportJobLifecycleService.fail(execution.jobId(), exception.getMessage());
    }
  }

  private Path generateFile(ExportJobLifecycleService.ExportJobExecution execution) throws IOException {
    if (execution.type() != ExportJobType.CSV) {
      throw new IllegalArgumentException("Unsupported export type: " + execution.type());
    }

    List<Task> tasks = taskRepository.findAllByProjectIdOrderByIdAsc(execution.projectId());
    return csvExportWriter.write(execution.jobId(), execution.projectId(), tasks);
  }
}
