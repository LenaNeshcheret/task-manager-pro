package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.domain.ExportJob;
import com.lenaneshcheret.taskmanager.domain.ExportJobStatus;
import com.lenaneshcheret.taskmanager.domain.ExportJobType;
import com.lenaneshcheret.taskmanager.repository.ExportJobRepository;
import com.lenaneshcheret.taskmanager.repository.ProjectRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ExportService {

  private final ExportJobRepository exportJobRepository;
  private final ProjectRepository projectRepository;
  private final CurrentUserService currentUserService;
  private final ExportAsyncWorker exportAsyncWorker;

  public ExportJob requestExport(Long projectId, ExportJobType type, JwtAuthenticationToken authentication) {
    Long ownerId = currentUserService.getOrCreateCurrentUser(authentication).getId();
    ensureOwnedProject(projectId, ownerId);
    ensureSupportedType(type);

    ExportJob exportJob = exportJobRepository.save(ExportJob.pending(ownerId, projectId, type));
    exportAsyncWorker.process(exportJob.getId());
    return exportJob;
  }

  public ExportJob getStatus(Long jobId, JwtAuthenticationToken authentication) {
    return findOwnedJob(jobId, authentication);
  }

  public ExportDownload download(Long jobId, JwtAuthenticationToken authentication) {
    ExportJob exportJob = findOwnedJob(jobId, authentication);
    if (exportJob.getStatus() != ExportJobStatus.DONE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Export is not ready for download");
    }
    if (exportJob.getFilePath() == null || exportJob.getFilePath().isBlank()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Export is not ready for download");
    }

    Path filePath = Path.of(exportJob.getFilePath());
    try {
      return new ExportDownload(
          "project-" + exportJob.getProjectId() + "-export-" + exportJob.getId() + ".csv",
          Files.readAllBytes(filePath)
      );
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Export file is unavailable");
    }
  }

  private ExportJob findOwnedJob(Long jobId, JwtAuthenticationToken authentication) {
    Long ownerId = currentUserService.getOrCreateCurrentUser(authentication).getId();
    return exportJobRepository.findByIdAndUserId(jobId, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Export job not found"));
  }

  private void ensureOwnedProject(Long projectId, Long ownerId) {
    projectRepository.findByIdAndOwnerId(projectId, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
  }

  private void ensureSupportedType(ExportJobType type) {
    if (type == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
    }
    if (type != ExportJobType.CSV) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CSV exports are supported");
    }
  }

  public record ExportDownload(String fileName, byte[] content) {
  }
}
