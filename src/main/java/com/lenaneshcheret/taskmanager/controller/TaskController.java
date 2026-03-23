package com.lenaneshcheret.taskmanager.controller;

import com.lenaneshcheret.taskmanager.controller.dto.TaskCreateRequest;
import com.lenaneshcheret.taskmanager.controller.dto.TaskResponse;
import com.lenaneshcheret.taskmanager.controller.dto.TaskUpdateRequest;
import com.lenaneshcheret.taskmanager.domain.TaskStatus;
import com.lenaneshcheret.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class TaskController {

  private final TaskService taskService;

  @PostMapping("/projects/{projectId}/tasks")
  public ResponseEntity<TaskResponse> create(
      @PathVariable Long projectId,
      @Valid @RequestBody TaskCreateRequest request,
      JwtAuthenticationToken authentication
  ) {
    TaskResponse response = TaskResponse.from(taskService.create(
        projectId,
        request.title(),
        request.description(),
        request.priority(),
        request.dueAt(),
        authentication
    ));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/projects/{projectId}/tasks")
  public Page<TaskResponse> list(
      @PathVariable Long projectId,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      Instant dueFrom,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      Instant dueTo,
      @RequestParam(required = false, name = "q") String queryText,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      JwtAuthenticationToken authentication
  ) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

    return taskService.list(projectId, status, dueFrom, dueTo, queryText, pageRequest, authentication)
        .map(TaskResponse::from);
  }

  @PatchMapping("/tasks/{taskId}")
  public TaskResponse update(
      @PathVariable Long taskId,
      @Valid @RequestBody TaskUpdateRequest request,
      JwtAuthenticationToken authentication
  ) {
    return TaskResponse.from(taskService.update(
        taskId,
        request.version(),
        request.title(),
        request.description(),
        request.status(),
        request.dueAt(),
        request.priority(),
        authentication
    ));
  }

  @PostMapping("/tasks/{taskId}/complete")
  public TaskResponse complete(@PathVariable Long taskId, JwtAuthenticationToken authentication) {
    return TaskResponse.from(taskService.complete(taskId, authentication));
  }

  @DeleteMapping("/tasks/{taskId}")
  public ResponseEntity<Void> delete(@PathVariable Long taskId, JwtAuthenticationToken authentication) {
    taskService.delete(taskId, authentication);
    return ResponseEntity.noContent().build();
  }
}
