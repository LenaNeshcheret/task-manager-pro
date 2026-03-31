package com.lenaneshcheret.taskmanager.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("reminderProperties")
@ConfigurationProperties(prefix = "app.reminders")
@Getter
@Setter
public class ReminderProperties {

  private boolean schedulerEnabled = true;

  private Duration dueWindow = Duration.ofMinutes(15);

  private long enqueueFixedDelayMs = 60_000L;

  private long dispatchFixedDelayMs = 5_000L;

  private int dispatchBatchSize = 100;
}
