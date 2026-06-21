package com.ilhanozkan.libraryManagementSystem.service.notification.impl;

import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.service.notification.EmailNotificationService;
import com.ilhanozkan.libraryManagementSystem.util.CustomDateTimeFormatter;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default JavaMail-based implementation. Renders plain-text bodies with a light HTML
 * wrapper (no template engine dependency, matching the project's minimal-deps style).
 *
 * SMTP errors are caught and logged at WARN - they are never propagated, so a mail
 * outage cannot break registration, availability, or overdue flows.
 *
 * NOTE: this class is NOT @Async itself. Callers that must stay responsive (registration,
 * availability) invoke it through @Async listeners; the daily overdue scheduler invokes
 * it directly on its own thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

  private final JavaMailSender mailSender;

  @Value("${notification.from-address:no-reply@library.local}")
  private String fromAddress;

  @Override
  public void sendRegistrationConfirmation(User user) {
    String subject = "Welcome to the Library, " + user.getName() + "!";
    String body = """
        <html><body style="font-family:Arial,sans-serif;line-height:1.5">
          <h2>Welcome to the Library, %s!</h2>
          <p>Your account has been created successfully.</p>
          <ul>
            <li><b>Username:</b> %s</li>
            <li><b>Email:</b> %s</li>
          </ul>
          <p>You can now browse the catalog and borrow books.</p>
          <p style="color:#888;font-size:12px">This is an automated message; please do not reply.</p>
        </body></html>
        """.formatted(
            escape(user.getName() + " " + user.getSurname()),
            escape(user.getUsername()),
            escape(user.getEmail()));

    send(user.getEmail(), subject, body);
  }

  @Override
  public void sendBookAvailable(User patron, String bookName, String isbn) {
    String subject = "Good news: \"" + bookName + "\" is available";
    String body = """
        <html><body style="font-family:Arial,sans-serif;line-height:1.5">
          <h2>A book on your waitlist is available!</h2>
          <p>Hi %s,</p>
          <p><b>"%s"</b> (ISBN %s) is now available for borrowing.</p>
          <p>Drop by the library or reserve it online before it goes out again.</p>
          <p style="color:#888;font-size:12px">This is an automated message; please do not reply.</p>
        </body></html>
        """.formatted(
            escape(patron.getName()),
            escape(bookName),
            escape(isbn == null ? "" : isbn));

    send(patron.getEmail(), subject, body);
  }

  @Override
  public void sendOverdueWarning(User patron, List<Borrowing> overdueBooks) {
    StringBuilder rows = new StringBuilder();
    for (Borrowing b : overdueBooks) {
      rows.append("<tr>")
          .append("<td>").append(escape(b.getBook() != null ? b.getBook().getName() : "?")).append("</td>")
          .append("<td>").append(CustomDateTimeFormatter.formatDateTime(b.getDueDate())).append("</td>")
          .append("</tr>");
    }

    String subject = "You have " + overdueBooks.size() + " overdue book(s)";
    String body = """
        <html><body style="font-family:Arial,sans-serif;line-height:1.5">
          <h2>Overdue book reminder</h2>
          <p>Hi %s,</p>
          <p>The following book(s) are past their due date. Please return them as soon as possible.</p>
          <table cellpadding="6" border="1" style="border-collapse:collapse">
            <tr><th>Book</th><th>Due date</th></tr>
            %s
          </table>
          <p style="color:#888;font-size:12px">This is an automated message; please do not reply.</p>
        </body></html>
        """.formatted(escape(patron.getName()), rows.toString());

    send(patron.getEmail(), subject, body);
  }

  /**
   * Single choke-point for actually sending mail. Swallows MailException so callers
   * are insulated from SMTP failures.
   */
  private void send(String to, String subject, String htmlBody) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      mailSender.send(message);
      log.info("Sent email to={} subject=\"{}\"", to, subject);
    } catch (Exception e) {
      // Best-effort: never propagate. Downstream DB writes (notifications stamps) are
      // intentionally still applied by callers to avoid retry storms.
      log.warn("Failed to send email to={} subject=\"{}\": {}", to, subject, e.getMessage());
    }
  }

  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
  }
}
