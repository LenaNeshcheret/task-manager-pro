package com.lenaneshcheret.taskmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

  private final ReminderService reminderService;

  @Scheduled(fixedDelayString = "#{@reminderProperties.enqueueFixedDelayMs}")
  public void enqueueDueSoonReminders() {
    reminderService.enqueueDueSoonReminders();
  }

  @Scheduled(fixedDelayString = "#{@reminderProperties.dispatchFixedDelayMs}")
  public void dispatchPendingReminders() {
    reminderService.dispatchPendingReminders();
  }
}
