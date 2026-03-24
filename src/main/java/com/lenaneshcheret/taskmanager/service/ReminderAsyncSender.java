package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.domain.Reminder;
import com.lenaneshcheret.taskmanager.domain.ReminderStatus;
import com.lenaneshcheret.taskmanager.repository.ReminderRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderAsyncSender {

  private static final int MAX_ERROR_LENGTH = 1_000;

  private final ReminderRepository reminderRepository;
  private final ReminderDeliveryGateway reminderDeliveryGateway;
  private final Clock clock;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void send(Long reminderId) {
    reminderRepository.findByIdForUpdate(reminderId)
        .filter(reminder -> reminder.getStatus() == ReminderStatus.PENDING)
        .ifPresent(this::sendReminder);
  }

  private void sendReminder(Reminder reminder) {
    try {
      reminderDeliveryGateway.send(reminder);
      reminder.markSent(clock.instant());
    } catch (RuntimeException exception) {
      reminder.markFailed(truncate(exception.getMessage()));
    }
  }

  private String truncate(String message) {
    if (message == null || message.isBlank()) {
      return "Reminder delivery failed";
    }
    return message.length() <= MAX_ERROR_LENGTH
        ? message
        : message.substring(0, MAX_ERROR_LENGTH);
  }
}
