package com.lenaneshcheret.taskmanager.repository;

import com.lenaneshcheret.taskmanager.domain.Reminder;
import com.lenaneshcheret.taskmanager.domain.ReminderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

  @Modifying
  @Query(value = """
      insert into task_reminders (task_id, scheduled_at, status, attempts, version)
      values (:taskId, :scheduledAt, 'PENDING', 0, 0)
      on conflict (task_id, scheduled_at) do nothing
      """, nativeQuery = true)
  int enqueuePending(@Param("taskId") Long taskId, @Param("scheduledAt") Instant scheduledAt);

  List<Reminder> findByStatusOrderByIdAsc(ReminderStatus status, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Reminder r where r.id = :id")
  Optional<Reminder> findByIdForUpdate(@Param("id") Long id);

  @Query("""
      select r
      from Reminder r, Task t, Project p
      where r.taskId = t.id
        and t.projectId = p.id
        and p.ownerId = :ownerId
      order by r.id asc
      """)
  List<Reminder> findAllByOwnerIdOrderByIdAsc(@Param("ownerId") Long ownerId);

  @Query("""
      select r
      from Reminder r, Task t, Project p
      where r.taskId = t.id
        and t.projectId = p.id
        and p.ownerId = :ownerId
        and r.status = :status
      order by r.id asc
      """)
  List<Reminder> findAllByOwnerIdAndStatusOrderByIdAsc(
      @Param("ownerId") Long ownerId,
      @Param("status") ReminderStatus status
  );
}
