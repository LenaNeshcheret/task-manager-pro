package com.lenaneshcheret.taskmanager.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  @GetMapping("/api/hello")
  public String hello() {
    return "task-manager-pro is running";
  }
}
