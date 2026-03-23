package com.lenaneshcheret.taskmanager.controller.dto;

import com.lenaneshcheret.taskmanager.domain.Reminder;
import com.lenaneshcheret.taskmanager.domain.ReminderStatus;
import java.time.Instant;

public record ReminderResponse(
    Long id,
    Long taskId,
    Instant scheduledAt,
    ReminderStatus status,
    int attempts,
    String lastError,
    Instant sentAt,
    Long version
) {

  public static ReminderResponse from(Reminder reminder) {
    return new ReminderResponse(
        reminder.getId(),
        reminder.getTaskId(),
        reminder.getScheduledAt(),
        reminder.getStatus(),
        reminder.getAttempts(),
        reminder.getLastError(),
        reminder.getSentAt(),
        reminder.getVersion()
    );
  }
}
