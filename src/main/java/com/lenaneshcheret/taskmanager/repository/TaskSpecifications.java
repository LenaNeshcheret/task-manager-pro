package com.lenaneshcheret.taskmanager.repository;

import com.lenaneshcheret.taskmanager.domain.Task;
import com.lenaneshcheret.taskmanager.domain.TaskStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecifications {

  private TaskSpecifications() {
  }

  public static Specification<Task> hasProjectId(Long projectId) {
    return (root, query, cb) ->
        projectId == null ? null : cb.equal(root.get("projectId"), projectId);
  }

  public static Specification<Task> hasStatus(TaskStatus status) {
    return (root, query, cb) ->
        status == null ? null : cb.equal(root.get("status"), status);
  }

  public static Specification<Task> dueAtFrom(Instant from) {
    return (root, query, cb) ->
        from == null ? null : cb.greaterThanOrEqualTo(root.get("dueAt"), from);
  }

  public static Specification<Task> dueAtTo(Instant to) {
    return (root, query, cb) ->
        to == null ? null : cb.lessThanOrEqualTo(root.get("dueAt"), to);
  }

  public static Specification<Task> textContains(String text) {
    return (root, query, cb) -> {
      if (text == null || text.isBlank()) {
        return null;
      }

      String pattern = "%" + text.toLowerCase() + "%";
      return cb.or(
          cb.like(cb.lower(root.get("title")), pattern),
          cb.like(cb.lower(root.get("description")), pattern)
      );
    };
  }

  public static Specification<Task> withFilters(
      Long projectId,
      TaskStatus status,
      Instant dueAtFrom,
      Instant dueAtTo,
      String text
  ) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (projectId != null) {
        predicates.add(cb.equal(root.get("projectId"), projectId));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (dueAtFrom != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("dueAt"), dueAtFrom));
      }
      if (dueAtTo != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("dueAt"), dueAtTo));
      }
      if (text != null && !text.isBlank()) {
        String pattern = "%" + text.toLowerCase() + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(root.get("title")), pattern),
            cb.like(cb.lower(root.get("description")), pattern)
        ));
      }

      return predicates.isEmpty() ? null : cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
