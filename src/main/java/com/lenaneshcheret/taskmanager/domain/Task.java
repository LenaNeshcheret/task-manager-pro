package com.lenaneshcheret.taskmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  @Setter
  private Long projectId;

  @Column(nullable = false, length = 200)
  @Setter
  private String title;

  @Column(columnDefinition = "TEXT")
  @Setter
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Setter
  private TaskStatus status;

  @Column(name = "due_at")
  @Setter
  private Instant dueAt;

  @Column(name = "completed_at")
  @Setter
  private Instant completedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Setter
  private TaskPriority priority;

  @Version
  @Column(nullable = false)
  private Long version;
}
