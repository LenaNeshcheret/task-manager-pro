package com.lenaneshcheret.taskmanager.controller;

import com.lenaneshcheret.taskmanager.controller.dto.ProjectRequest;
import com.lenaneshcheret.taskmanager.controller.dto.ProjectResponse;
import com.lenaneshcheret.taskmanager.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;

  @PostMapping
  public ResponseEntity<ProjectResponse> create(
      @Valid @RequestBody ProjectRequest request,
      JwtAuthenticationToken authentication
  ) {
    ProjectResponse response = ProjectResponse.from(projectService.create(request.name(), authentication));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public List<ProjectResponse> list(JwtAuthenticationToken authentication) {
    return projectService.list(authentication).stream()
        .map(ProjectResponse::from)
        .toList();
  }

  @GetMapping("/{id}")
  public ProjectResponse getById(@PathVariable Long id, JwtAuthenticationToken authentication) {
    return ProjectResponse.from(projectService.getById(id, authentication));
  }

  @PatchMapping("/{id}")
  public ProjectResponse update(
      @PathVariable Long id,
      @Valid @RequestBody ProjectRequest request,
      JwtAuthenticationToken authentication
  ) {
    return ProjectResponse.from(projectService.update(id, request.name(), authentication));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, JwtAuthenticationToken authentication) {
    projectService.delete(id, authentication);
    return ResponseEntity.noContent().build();
  }
}
