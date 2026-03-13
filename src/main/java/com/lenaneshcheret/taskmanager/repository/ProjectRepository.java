package com.lenaneshcheret.taskmanager.repository;

import com.lenaneshcheret.taskmanager.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  List<Project> findAllByOwnerIdOrderByIdAsc(Long ownerId);

  Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
}
