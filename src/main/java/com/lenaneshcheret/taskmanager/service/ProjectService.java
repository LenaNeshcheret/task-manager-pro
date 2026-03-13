package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.domain.Project;
import com.lenaneshcheret.taskmanager.domain.User;
import com.lenaneshcheret.taskmanager.repository.ProjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final CurrentUserService currentUserService;

  @Transactional
  public Project create(String name, JwtAuthenticationToken authentication) {
    User user = currentUserService.getOrCreateCurrentUser(authentication);
    Project project = Project.create(user.getId(), normalizeName(name));
    return projectRepository.save(project);
  }

  public List<Project> list(JwtAuthenticationToken authentication) {
    Long ownerId = currentUserService.getOrCreateCurrentUser(authentication).getId();
    return projectRepository.findAllByOwnerIdOrderByIdAsc(ownerId);
  }

  public Project getById(Long id, JwtAuthenticationToken authentication) {
    return findOwnedProject(id, authentication);
  }

  @Transactional
  public Project update(Long id, String name, JwtAuthenticationToken authentication) {
    Project project = findOwnedProject(id, authentication);
    project.setName(normalizeName(name));
    return project;
  }

  @Transactional
  public void delete(Long id, JwtAuthenticationToken authentication) {
    Project project = findOwnedProject(id, authentication);
    projectRepository.delete(project);
  }

  private Project findOwnedProject(Long id, JwtAuthenticationToken authentication) {
    Long ownerId = currentUserService.getOrCreateCurrentUser(authentication).getId();
    return projectRepository.findByIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
  }

  private String normalizeName(String name) {
    return name == null ? null : name.trim();
  }
}
