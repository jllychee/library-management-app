package com.ilhanozkan.libraryManagementSystem.service.notification;

import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;

import java.util.List;

/**
 * Sends transactional email notifications for critical library events.
 *
 * All methods are best-effort: SMTP failures must not propagate to callers
 * (notifications must never break a registration, return, or availability flow).
 * Implementations should log failures at WARN and return normally.
 */
public interface EmailNotificationService {

  /**
   * Welcome / confirmation email sent after a successful registration.
   */
  void sendRegistrationConfirmation(User user);

  /**
   * "The book you waitlisted is available" email, sent to one patron.
   *
   * @param patron     the recipient
   * @param bookName   human-readable book name
   * @param isbn       book ISBN (may be included in the body)
   */
  void sendBookAvailable(User patron, String bookName, String isbn);

  /**
   * Overdue digest sent to a patron. Groups multiple overdue borrowings into one email.
   *
   * @param patron       the recipient
   * @param overdueBooks non-empty list of that patron's overdue borrowings
   */
  void sendOverdueWarning(User patron, List<Borrowing> overdueBooks);
}
