package com.ilhanozkan.libraryManagementSystem.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A patron's request to be notified when a previously-unavailable book becomes available
 * again. When the book transitions from availableQuantity 0 to >0, the availability
 * listener notifies each waitlisted user once and stamps {@link #notifiedAt}.
 *
 * Unique on (user, book) so a patron can waitlist a given title only once.
 *
 * Both FKs are ON DELETE CASCADE: deleting a book or user automatically drops their
 * waitlist rows (no orphans, and integration tests that wipe books/users via deleteAll
 * don't need to know about this table).
 */
@Entity
@Table(
    name = "book_waitlist",
    uniqueConstraints = @UniqueConstraint(name = "uk_waitlist_user_book", columnNames = {"user_id", "book_id"})
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookWaitlist {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Book book;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  // Stamped after the user has been emailed that the book became available.
  // Null means the user has not yet been notified for this entry.
  @Column
  private LocalDateTime notifiedAt;

  @PrePersist
  public void onCreate() {
    if (this.id == null)
      this.id = UUID.randomUUID();
    if (this.createdAt == null)
      this.createdAt = LocalDateTime.now();
  }
}
