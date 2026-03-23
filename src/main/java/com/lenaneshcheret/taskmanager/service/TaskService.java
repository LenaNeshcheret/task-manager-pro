package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.domain.Task;
import com.lenaneshcheret.taskmanager.domain.TaskPriority;
import com.lenaneshcheret.taskmanager.domain.TaskStatus;
import com.lenaneshcheret.taskmanager.repository.ProjectRepository;
import com.lenaneshcheret.taskmanager.repository.TaskRepository;
import com.lenaneshcheret.taskmanager.repository.TaskSpecifications;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final CurrentUserService currentUserService;

  @Transactional
  public Task create(
      Long projectId,
      String title,
      String description,
      TaskPriority priority,
      Instant dueAt,
      JwtAuthenticationToken authentication
  ) {
    Long ownerId = currentOwnerId(authentication);
    ensureOwnedProject(projectId, ownerId);

    Task task = Task.create(
        projectId,
        normalizeTitle(title),
        normalizeDescription(description),
        priority == null ? TaskPriority.MEDIUM : priority,
        dueAt
    );

    return taskRepository.save(task);
  }

  public Page<Task> list(
      Long projectId,
      TaskStatus status,
      Instant dueFrom,
      Instant dueTo,
      String queryText,
      Pageable pageable,
      JwtAuthenticationToken authentication
  ) {
    Long ownerId = currentOwnerId(authentication);
    ensureOwnedProject(projectId, ownerId);

    if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueFrom must be before or equal to dueTo");
    }

    return taskRepository.findAll(
        TaskSpecifications.withFilters(projectId, status, dueFrom, dueTo, queryText),
        pageable
    );
  }

  @Transactional
  public Task update(
      Long taskId,
      Long version,
      String title,
      String description,
      TaskStatus status,
      Instant dueAt,
      TaskPriority priority,
      JwtAuthenticationToken authentication
  ) {
    Long ownerId = currentOwnerId(authentication);
    Task task = findOwnedTask(taskId, ownerId);

    ensureMatchingVersion(version, task.getVersion());

    if (title != null) {
      task.setTitle(normalizeTitle(title));
    }
    if (description != null) {
      task.setDescription(normalizeDescription(description));
    }
    if (status != null) {
      task.setStatus(status);
      if (status == TaskStatus.DONE) {
        if (task.getCompletedAt() == null) {
          task.setCompletedAt(Instant.now());
        }
      } else {
        task.setCompletedAt(null);
      }
    }
    if (dueAt != null) {
      task.setDueAt(dueAt);
    }
    if (priority != null) {
      task.setPriority(priority);
    }

    return saveAndFlush(task);
  }

  @Transactional
  public Task complete(Long taskId, JwtAuthenticationToken authentication) {
    Long ownerId = currentOwnerId(authentication);
    Task task = findOwnedTask(taskId, ownerId);

    task.setStatus(TaskStatus.DONE);
    task.setCompletedAt(Instant.now());

    return saveAndFlush(task);
  }

  @Transactional
  public void delete(Long taskId, JwtAuthenticationToken authentication) {
    Long ownerId = currentOwnerId(authentication);
    Task task = findOwnedTask(taskId, ownerId);
    taskRepository.delete(task);
  }

  private Task saveAndFlush(Task task) {
    try {
      return taskRepository.saveAndFlush(task);
    } catch (OptimisticLockingFailureException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Task update conflict");
    }
  }

  private Long currentOwnerId(JwtAuthenticationToken authentication) {
    return currentUserService.getOrCreateCurrentUser(authentication).getId();
  }

  private void ensureOwnedProject(Long projectId, Long ownerId) {
    projectRepository.findByIdAndOwnerId(projectId, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
  }

  private Task findOwnedTask(Long taskId, Long ownerId) {
    return taskRepository.findByIdAndOwnerId(taskId, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
  }

  private void ensureMatchingVersion(Long requestedVersion, Long currentVersion) {
    if (requestedVersion == null || !requestedVersion.equals(currentVersion)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Task update conflict");
    }
  }

  private String normalizeTitle(String title) {
    String normalized = title == null ? null : title.trim();
    if (normalized == null || normalized.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
    }
    return normalized;
  }

  private String normalizeDescription(String description) {
    return description == null ? null : description.trim();
  }
}
