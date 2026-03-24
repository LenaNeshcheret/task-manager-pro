package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.domain.Reminder;

public interface ReminderDeliveryGateway {

  void send(Reminder reminder);
}
