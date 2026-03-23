package com.lenaneshcheret.taskmanager.repository;

import com.lenaneshcheret.taskmanager.domain.Task;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

  @Query("""
      select t
      from Task t, Project p
      where t.id = :taskId
        and p.id = t.projectId
        and p.ownerId = :ownerId
      """)
  Optional<Task> findByIdAndOwnerId(@Param("taskId") Long taskId, @Param("ownerId") Long ownerId);
}
