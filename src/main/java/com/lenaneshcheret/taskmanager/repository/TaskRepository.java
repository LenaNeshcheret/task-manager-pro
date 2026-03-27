package com.lenaneshcheret.taskmanager.repository;

import com.lenaneshcheret.taskmanager.domain.Task;
import com.lenaneshcheret.taskmanager.domain.TaskStatus;
import java.time.Instant;
import java.util.List;
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

  @Query("""
      select t
      from Task t
      where t.status <> :completedStatus
        and t.dueAt is not null
        and t.dueAt >= :windowStart
        and t.dueAt <= :windowEnd
      order by t.dueAt asc, t.id asc
      """)
  List<Task> findDueWithinWindow(
      @Param("windowStart") Instant windowStart,
      @Param("windowEnd") Instant windowEnd,
      @Param("completedStatus") TaskStatus completedStatus
  );

  List<Task> findAllByProjectIdOrderByIdAsc(Long projectId);
}
