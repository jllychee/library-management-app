package com.ilhanozkan.libraryManagementSystem.service.notification;

import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Daily job that emails each patron a digest of their overdue books, then stamps
 * {@link Borrowing#getLastOverdueNotifiedAt()} so the same overdue book is not re-emailed
 * every tick (controlled by notification.overdue-remind-days).
 *
 * Runs on the scheduler thread (single-threaded by default). SMTP here is synchronous,
 * which is fine for a once-daily batch; user-facing notification paths use @Async listeners.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueNotificationScheduler {

  private final BorrowingRepository borrowingRepository;
  private final EmailNotificationService emailService;

  @Value("${notification.overdue-remind-days:3}")
  private long remindDays;

  /**
   * Also exposed as a plain method so tests / a librarian "send reminders now" action
   * can trigger it without waiting for the cron.
   */
  @Scheduled(cron = "${notification.overdue-cron:0 0 8 * * *}")
  @Transactional
  public void sendOverdueNotifications() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(remindDays);
    List<Borrowing> due = borrowingRepository.findOverdueBooksNeedingReminder(cutoff);
    if (due.isEmpty()) {
      log.debug("Overdue scan: nothing to send");
      return;
    }

    // Group borrowings by user id to send one digest per patron.
    Map<UUID, List<Borrowing>> byUser = new HashMap<>();
    Map<UUID, User> userById = new HashMap<>();
    for (Borrowing b : due) {
      User u = b.getUser();
      byUser.computeIfAbsent(u.getId(), k -> new ArrayList<>()).add(b);
      userById.putIfAbsent(u.getId(), u);
    }

    log.info("Overdue scan: notifying {} patron(s) across {} borrowing(s)", byUser.size(), due.size());
    LocalDateTime now = LocalDateTime.now();
    for (Map.Entry<UUID, List<Borrowing>> entry : byUser.entrySet()) {
      User patron = userById.get(entry.getKey());
      emailService.sendOverdueWarning(patron, entry.getValue());
      for (Borrowing b : entry.getValue()) {
        b.setLastOverdueNotifiedAt(now);
        borrowingRepository.save(b);
      }
    }
  }
}
