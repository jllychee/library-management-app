package com.ilhanozkan.libraryManagementSystem.service.notification;

import com.ilhanozkan.libraryManagementSystem.config.AsyncConfig;
import com.ilhanozkan.libraryManagementSystem.model.dto.event.BookBecameAvailableEvent;
import com.ilhanozkan.libraryManagementSystem.model.entity.BookWaitlist;
import com.ilhanozkan.libraryManagementSystem.repository.BookWaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * When a book's available quantity transitions from 0 to >0, email each patron who
 * waitlisted that book (once) and stamp notifiedAt so they aren't emailed again for
 * the same entry.
 *
 * AFTER_COMMIT + @Async: the @Async advisor submits to the notification executor, and
 * on that thread we run in a REQUIRES_NEW transaction so lazy access to each entry's
 * user (for the email address) has an open Hibernate session. (Spring 6.1 requires
 * REQUIRES_NEW or NOT_SUPPORTED when combining @Transactional with @TransactionalEventListener.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookAvailabilityNotificationListener {

  private final BookWaitlistRepository waitlistRepository;
  private final EmailNotificationService emailService;

  @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onBookBecameAvailable(BookBecameAvailableEvent event) {
    List<BookWaitlist> pending = waitlistRepository.findByBookIdAndNotifiedAtIsNull(event.bookId());
    if (pending.isEmpty()) {
      log.debug("No waitlisters to notify for bookId={}", event.bookId());
      return;
    }
    log.info("Notifying {} waitlister(s) for bookId={}", pending.size(), event.bookId());

    LocalDateTime now = LocalDateTime.now();
    for (BookWaitlist entry : pending) {
      // Best-effort: sendBookAvailable swallows SMTP errors. We stamp notifiedAt
      // regardless to avoid retry storms (consistent with the overdue path).
      emailService.sendBookAvailable(entry.getUser(), event.bookName(), event.isbn());
      entry.setNotifiedAt(now);
      waitlistRepository.save(entry);
    }
  }
}
