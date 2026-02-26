package com.lenaneshcheret.taskmanager.repository;

import com.lenaneshcheret.taskmanager.domain.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {
}
