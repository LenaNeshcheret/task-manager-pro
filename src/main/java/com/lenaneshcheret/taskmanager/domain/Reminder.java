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
@Table(name = "task_reminders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "task_id", nullable = false)
  @Setter
  private Long taskId;

  @Column(name = "scheduled_at", nullable = false)
  @Setter
  private Instant scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Setter
  private ReminderStatus status;

  @Column(nullable = false)
  @Setter
  private int attempts;

  @Column(name = "last_error", columnDefinition = "TEXT")
  @Setter
  private String lastError;

  @Column(name = "sent_at")
  @Setter
  private Instant sentAt;

  @Version
  @Column(nullable = false)
  private Long version;

  public void markSent(Instant sentAt) {
    status = ReminderStatus.SENT;
    lastError = null;
    this.sentAt = sentAt;
  }

  public void markFailed(String errorMessage) {
    attempts += 1;
    status = ReminderStatus.FAILED;
    lastError = errorMessage;
    sentAt = null;
  }
}
