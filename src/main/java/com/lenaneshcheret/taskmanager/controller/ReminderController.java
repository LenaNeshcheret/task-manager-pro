package com.lenaneshcheret.taskmanager.controller;

import com.lenaneshcheret.taskmanager.controller.dto.ReminderResponse;
import com.lenaneshcheret.taskmanager.domain.ReminderStatus;
import com.lenaneshcheret.taskmanager.service.ReminderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ReminderController {

  private final ReminderService reminderService;

  @GetMapping
  public List<ReminderResponse> list(
      @RequestParam(required = false) ReminderStatus status,
      JwtAuthenticationToken authentication
  ) {
    return reminderService.list(status, authentication).stream()
        .map(ReminderResponse::from)
        .toList();
  }
}
