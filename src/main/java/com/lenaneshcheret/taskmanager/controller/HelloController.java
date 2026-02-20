package com.lenaneshcheret.taskmanager.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  @GetMapping("/api/health")
  public String health() {
    return "task-manager-pro is running";
  }
}
