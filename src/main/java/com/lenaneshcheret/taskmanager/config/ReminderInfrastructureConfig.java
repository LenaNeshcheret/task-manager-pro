package com.lenaneshcheret.taskmanager.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class ReminderInfrastructureConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
