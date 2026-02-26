package com.lenaneshcheret.taskmanager.repository;

import com.lenaneshcheret.taskmanager.domain.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
}
