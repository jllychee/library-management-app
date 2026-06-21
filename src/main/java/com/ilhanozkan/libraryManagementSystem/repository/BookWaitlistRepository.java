package com.ilhanozkan.libraryManagementSystem.repository;

import com.ilhanozkan.libraryManagementSystem.model.entity.Book;
import com.ilhanozkan.libraryManagementSystem.model.entity.BookWaitlist;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookWaitlistRepository extends JpaRepository<BookWaitlist, UUID> {

  // Entries still awaiting notification for a given book.
  List<BookWaitlist> findByBookAndNotifiedAtIsNull(Book book);

  // Same, looked up by book id (avoids loading the Book entity in async listeners).
  List<BookWaitlist> findByBookIdAndNotifiedAtIsNull(UUID bookId);

  // All of a user's waitlist entries (any notification state).
  List<BookWaitlist> findByUser(User user);

  boolean existsByUserAndBook(User user, Book book);

  Optional<BookWaitlist> findByUserAndBook(User user, Book book);
}
