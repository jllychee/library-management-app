package com.ilhanozkan.libraryManagementSystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Mail configuration.
 *
 * In non-test profiles, Spring Boot's {@code MailSenderAutoConfiguration} supplies the
 * {@link JavaMailSender} from the {@code spring.mail.*} properties in application.yml.
 *
 * In the {@code test} profile we register a capturing no-op JavaMailSender so that
 * {@code ./mvnw test} stays hermetic (no SMTP traffic, no network). Tests can cast the
 * autowired JavaMailSender to {@link CapturingJavaMailSender} to assert on sent mail.
 */
@Configuration
public class MailConfig {

    @Bean
    @Profile("test")
    public JavaMailSender testJavaMailSender() {
        return new CapturingJavaMailSender();
    }

    /**
     * A JavaMailSender that never talks to a real SMTP server. It builds real
     * {@link MimeMessage} objects (backed by a disconnected Session) and records them
     * in-memory for test inspection. All send calls succeed silently.
     */
    public static class CapturingJavaMailSender implements JavaMailSender {
        private final Session session = Session.getInstance(new Properties());
        private final List<MimeMessage> sent = Collections.synchronizedList(new ArrayList<>());

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(session);
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            try {
                return new MimeMessage(session, contentStream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            sent.add(mimeMessage);
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
            Collections.addAll(sent, mimeMessages);
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            MimeMessage m = createMimeMessage();
            try {
                if (simpleMessage.getTo() != null && simpleMessage.getTo().length > 0) {
                    m.setRecipients(MimeMessage.RecipientType.TO, String.join(",", simpleMessage.getTo()));
                }
                if (simpleMessage.getSubject() != null) {
                    m.setSubject(simpleMessage.getSubject());
                }
                if (simpleMessage.getText() != null) {
                    m.setText(simpleMessage.getText());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            sent.add(m);
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            for (SimpleMailMessage m : simpleMessages) {
                send(m);
            }
        }

        public List<MimeMessage> getSentMessages() {
            return Collections.unmodifiableList(sent);
        }

        public void clear() {
            sent.clear();
        }
    }
}
