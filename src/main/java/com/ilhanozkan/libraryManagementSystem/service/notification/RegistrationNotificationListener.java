package com.ilhanozkan.libraryManagementSystem.service.notification;

import com.ilhanozkan.libraryManagementSystem.config.AsyncConfig;
import com.ilhanozkan.libraryManagementSystem.model.dto.event.UserRegisteredEvent;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the registration confirmation email after the new user row is durably committed.
 *
 * Uses AFTER_COMMIT so a rollback (e.g. constraint violation downstream) never produces a
 * spurious "welcome" email. @Async offloads SMTP from the request thread.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationNotificationListener {

  private final EmailNotificationService emailService;

  @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onUserRegistered(UserRegisteredEvent event) {
    log.debug("Handling registration notification for userId={}", event.userId());
    User user = User.builder()
        .id(event.userId())
        .username(event.username())
        .email(event.email())
        .name(event.name())
        .surname(event.surname())
        .build();
    emailService.sendRegistrationConfirmation(user);
  }
}
