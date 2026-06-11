package com.ilhanozkan.libraryManagementSystem.model.entity;

import com.ilhanozkan.libraryManagementSystem.model.enums.FineStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "fines",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fines_borrowing", columnNames = "borrowing_id")
    }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Fine {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @OneToOne
  @JoinColumn(name = "borrowing_id", nullable = false, unique = true)
  private Borrowing borrowing;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FineStatus status;

  @Column(nullable = false)
  private Long overdueDays;

  @Column(nullable = false)
  private LocalDateTime calculatedUntil;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void onCreate() {
    if (this.id == null)
      this.id = UUID.randomUUID();

    if (this.status == null)
      this.status = FineStatus.UNPAID;

    if (this.createdAt == null)
      this.createdAt = LocalDateTime.now();

    if (this.updatedAt == null)
      this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
