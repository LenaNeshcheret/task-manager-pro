package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.domain.Reminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingReminderDeliveryGateway implements ReminderDeliveryGateway {

  private static final Logger log = LoggerFactory.getLogger(LoggingReminderDeliveryGateway.class);

  @Override
  public void send(Reminder reminder) {
    log.info("Sent reminder {} for task {}", reminder.getId(), reminder.getTaskId());
  }
}
