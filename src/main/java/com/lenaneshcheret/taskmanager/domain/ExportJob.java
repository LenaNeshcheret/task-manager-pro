package com.lenaneshcheret.taskmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "export_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExportJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Setter
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Setter
  private ExportJobStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Setter
  private ExportJobType type;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "finished_at")
  @Setter
  private Instant finishedAt;

  @Column(name = "file_path", length = 1024)
  @Setter
  private String filePath;

  @Version
  @Column(nullable = false)
  private Long version;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
