package com.ilhanozkan.libraryManagementSystem.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "borrowings")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Borrowing {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @CreatedDate
  @Column(nullable = false)
  private LocalDateTime borrowDate;

  @Column(nullable = false)
  private LocalDateTime dueDate;

  @Column
  private LocalDateTime returnDate;

  @Column(nullable = false)
  private Boolean returned;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // Timestamp of the last overdue-warning email sent for this borrowing.
  // Nullable; when null the borrowing has never been notified. Used by the
  // OverdueNotificationScheduler to avoid re-emailing the same overdue book every run.
  @Column
  private LocalDateTime lastOverdueNotifiedAt;

  // Expiration duration (days)
  private static final int expirationDuration = 14;

  @PrePersist
  public void onCreate() {
    if (this.id == null)
      this.id = UUID.randomUUID();

    if (borrowDate == null)
      borrowDate = LocalDateTime.now();

    // Set default due date to 14 days from now
    if (dueDate == null)
      dueDate = LocalDateTime.now().plusDays(expirationDuration);

    if (returned == null)
      returned = false;

    if (updatedAt == null)
      updatedAt = LocalDateTime.now();
  }
}
