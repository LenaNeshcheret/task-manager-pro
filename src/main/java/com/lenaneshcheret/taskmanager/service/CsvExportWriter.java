package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.config.ExportProperties;
import com.lenaneshcheret.taskmanager.domain.Task;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CsvExportWriter {

  private final ExportProperties exportProperties;

  public Path write(Long jobId, Long projectId, List<Task> tasks) throws IOException {
    Path storageDir = Path.of(exportProperties.getStorageDir());
    Files.createDirectories(storageDir);

    Path csvPath = storageDir.resolve("project-" + projectId + "-export-" + jobId + ".csv");
    Files.writeString(
        csvPath,
        render(tasks),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
    );
    return csvPath;
  }

  private String render(List<Task> tasks) {
    StringBuilder csv = new StringBuilder();
    csv.append("id,title,description,status,dueAt,completedAt,priority").append('\n');

    for (Task task : tasks) {
      csv.append(task.getId()).append(',')
          .append(escape(task.getTitle())).append(',')
          .append(escape(task.getDescription())).append(',')
          .append(task.getStatus()).append(',')
          .append(value(task.getDueAt())).append(',')
          .append(value(task.getCompletedAt())).append(',')
          .append(task.getPriority())
          .append('\n');
    }

    return csv.toString();
  }

  private String value(Object value) {
    return value == null ? "" : escape(value.toString());
  }

  private String escape(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
