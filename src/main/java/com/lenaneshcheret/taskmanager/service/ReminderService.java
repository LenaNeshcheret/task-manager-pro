package com.lenaneshcheret.taskmanager.service;

import com.lenaneshcheret.taskmanager.config.ReminderProperties;
import com.lenaneshcheret.taskmanager.domain.Reminder;
import com.lenaneshcheret.taskmanager.domain.ReminderStatus;
import com.lenaneshcheret.taskmanager.domain.Task;
import com.lenaneshcheret.taskmanager.domain.TaskStatus;
import com.lenaneshcheret.taskmanager.repository.ReminderRepository;
import com.lenaneshcheret.taskmanager.repository.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderService {

  private final ReminderRepository reminderRepository;
  private final TaskRepository taskRepository;
  private final CurrentUserService currentUserService;
  private final ReminderProperties reminderProperties;
  private final ReminderAsyncSender reminderAsyncSender;
  private final Clock clock;

  @Transactional
  public int enqueueDueSoonReminders() {
    Instant now = clock.instant();
    Instant windowEnd = now.plus(reminderProperties.getDueWindow());

    List<Task> dueSoonTasks = taskRepository.findDueWithinWindow(now, windowEnd, TaskStatus.DONE);
    int created = 0;

    for (Task task : dueSoonTasks) {
      created += reminderRepository.enqueuePending(task.getId(), task.getDueAt());
    }

    return created;
  }

  public void dispatchPendingReminders() {
    List<Reminder> pendingReminders = reminderRepository.findByStatusOrderByIdAsc(
        ReminderStatus.PENDING,
        PageRequest.of(0, reminderProperties.getDispatchBatchSize())
    );

    pendingReminders.forEach(reminder -> reminderAsyncSender.send(reminder.getId()));
  }

  public List<Reminder> list(ReminderStatus status, JwtAuthenticationToken authentication) {
    Long ownerId = currentUserService.getOrCreateCurrentUser(authentication).getId();
    if (status == null) {
      return reminderRepository.findAllByOwnerIdOrderByIdAsc(ownerId);
    }
    return reminderRepository.findAllByOwnerIdAndStatusOrderByIdAsc(ownerId, status);
  }
}
