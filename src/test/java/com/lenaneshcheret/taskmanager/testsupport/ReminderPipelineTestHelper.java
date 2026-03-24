package com.lenaneshcheret.taskmanager.testsupport;

import com.lenaneshcheret.taskmanager.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
@RequiredArgsConstructor
public class ReminderPipelineTestHelper {

  private final ReminderService reminderService;

  public int enqueueDueSoonReminders() {
    return reminderService.enqueueDueSoonReminders();
  }
}
