package com.ilhanozkan.libraryManagementSystem.repository;

import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BorrowingRepository extends JpaRepository<Borrowing, UUID> {
  List<Borrowing> findByUserAndReturnedFalse(User user);
  List<Borrowing> findByUser(User user);

  // Find all borrowings that are not returned and are overdue
  @Query(
      value = "SELECT b FROM Borrowing b WHERE b.returned = false AND b.dueDate < CURRENT_DATE"
  )
  List<Borrowing> findOverdueBooks();

  // Overdue borrowings that are due for a reminder: either never notified, or last
  // notified before `cutoff`. Used by OverdueNotificationScheduler for dedup.
  @Query(
      "SELECT b FROM Borrowing b WHERE b.returned = false " +
      "AND b.dueDate < CURRENT_TIMESTAMP " +
      "AND (b.lastOverdueNotifiedAt IS NULL OR b.lastOverdueNotifiedAt < :cutoff)"
  )
  List<Borrowing> findOverdueBooksNeedingReminder(@Param("cutoff") LocalDateTime cutoff);
}
