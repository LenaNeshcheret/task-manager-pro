package com.lenaneshcheret.taskmanager.repository;

import com.lenaneshcheret.taskmanager.domain.ExportJob;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {

  Optional<ExportJob> findByIdAndUserId(Long id, Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ExportJob e where e.id = :jobId")
  Optional<ExportJob> findByIdForUpdate(@Param("jobId") Long jobId);
}
