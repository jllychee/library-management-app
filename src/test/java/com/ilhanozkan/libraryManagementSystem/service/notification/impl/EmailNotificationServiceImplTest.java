package com.ilhanozkan.libraryManagementSystem.service.notification.impl;

import com.ilhanozkan.libraryManagementSystem.config.MailConfig.CapturingJavaMailSender;
import com.ilhanozkan.libraryManagementSystem.model.entity.Book;
import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.enums.BookGenre;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserRole;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserStatus;
import com.ilhanozkan.libraryManagementSystem.service.notification.EmailNotificationService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for {@link EmailNotificationServiceImpl} using the in-memory
 * {@link CapturingJavaMailSender} (the same bean wired in the test profile) to assert on
 * the actual composed MimeMessage content. No Spring context, no SMTP.
 */
class EmailNotificationServiceImplTest {

    private CapturingJavaMailSender sender;
    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        sender = new CapturingJavaMailSender();
        service = new EmailNotificationServiceImpl(sender);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@library.local");
    }

    @Test
    void shouldSendRegistrationEmailWithUserDetails() throws Exception {
        User user = User.builder()
            .id(UUID.randomUUID())
            .username("alice")
            .email("alice@example.com")
            .name("Alice")
            .surname("Smith")
            .role(UserRole.PATRON)
            .status(UserStatus.ACTIVE)
            .build();

        service.sendRegistrationConfirmation(user);

        assertThat(sender.getSentMessages()).hasSize(1);
        MimeMessage m = sender.getSentMessages().get(0);
        assertThat(m.getRecipients(jakarta.mail.Message.RecipientType.TO)[0].toString()).isEqualTo("alice@example.com");
        assertThat(m.getSubject()).contains("Welcome");
        String content = (String) m.getContent();
        assertThat(content).contains("Alice Smith");
        assertThat(content).contains("alice");
    }

    @Test
    void shouldSendBookAvailableEmail() throws Exception {
        User patron = User.builder()
            .id(UUID.randomUUID())
            .username("bob")
            .email("bob@example.com")
            .name("Bob")
            .surname("Jones")
            .build();

        service.sendBookAvailable(patron, "Dune", "9780441172719");

        assertThat(sender.getSentMessages()).hasSize(1);
        MimeMessage m = sender.getSentMessages().get(0);
        assertThat(m.getSubject()).contains("Dune");
        String content = (String) m.getContent();
        assertThat(content).contains("Dune");
        assertThat(content).contains("9780441172719");
    }

    @Test
    void shouldSendOverdueDigestWithAllBooks() throws Exception {
        User patron = User.builder()
            .id(UUID.randomUUID())
            .username("carol")
            .email("carol@example.com")
            .name("Carol")
            .build();
        Book b1 = Book.builder()
            .name("Book One").isbn("9780000000001").author("A").publisher("P")
            .numberOfPages(1).quantity(1).availableQuantity(1).genre(BookGenre.OTHER).build();
        Book b2 = Book.builder()
            .name("Book Two").isbn("9780000000002").author("A").publisher("P")
            .numberOfPages(1).quantity(1).availableQuantity(1).genre(BookGenre.OTHER).build();

        Borrowing ov1 = Borrowing.builder()
            .book(b1).user(patron).borrowDate(LocalDateTime.now().minusDays(30))
            .dueDate(LocalDateTime.now().minusDays(10)).returned(false)
            .updatedAt(LocalDateTime.now()).build();
        Borrowing ov2 = Borrowing.builder()
            .book(b2).user(patron).borrowDate(LocalDateTime.now().minusDays(30))
            .dueDate(LocalDateTime.now().minusDays(5)).returned(false)
            .updatedAt(LocalDateTime.now()).build();

        service.sendOverdueWarning(patron, List.of(ov1, ov2));

        assertThat(sender.getSentMessages()).hasSize(1);
        MimeMessage m = sender.getSentMessages().get(0);
        assertThat(m.getSubject()).contains("2 overdue");
        String content = (String) m.getContent();
        assertThat(content).contains("Book One");
        assertThat(content).contains("Book Two");
    }

    @Test
    void sendFailureShouldNeverPropagate() {
        // A JavaMailSender that throws on send must NOT break the caller.
        CapturingJavaMailSender throwing = new CapturingJavaMailSender() {
            @Override
            public void send(MimeMessage mimeMessage) {
                throw new RuntimeException("SMTP down");
            }
        };
        EmailNotificationService throwingService = new EmailNotificationServiceImpl(throwing);
        ReflectionTestUtils.setField(throwingService, "fromAddress", "noreply@library.local");

        // Must not throw - best-effort notification contract.
        throwingService.sendRegistrationConfirmation(
            User.builder().username("x").email("x@example.com").name("X").surname("Y").build());
        // No exception = pass.
    }
}
