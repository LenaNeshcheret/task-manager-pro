package com.lenaneshcheret.taskmanager.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 320, unique = true)
  @Setter
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  @Setter
  private String passwordHash;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      uniqueConstraints = @UniqueConstraint(
          name = "uk_user_roles_user_id_role",
          columnNames = {"user_id", "role"}
      )
  )
  @Column(name = "role", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private Set<UserRole> roles = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Version
  @Column(nullable = false)
  private Long version;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public void setRoles(Set<UserRole> roles) {
    this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
  }
}
