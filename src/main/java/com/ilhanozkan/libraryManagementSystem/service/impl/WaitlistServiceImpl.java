package com.ilhanozkan.libraryManagementSystem.service.impl;

import com.ilhanozkan.libraryManagementSystem.common.exception.book.BookNotFoundException;
import com.ilhanozkan.libraryManagementSystem.model.dto.response.BookWaitlistResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.Book;
import com.ilhanozkan.libraryManagementSystem.model.entity.BookWaitlist;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.mapper.BookWaitlistResponseDTOMapper;
import com.ilhanozkan.libraryManagementSystem.repository.BookRepository;
import com.ilhanozkan.libraryManagementSystem.repository.BookWaitlistRepository;
import com.ilhanozkan.libraryManagementSystem.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistServiceImpl implements WaitlistService {

  private final BookWaitlistRepository waitlistRepository;
  private final BookRepository bookRepository;
  private final BookWaitlistResponseDTOMapper mapper = BookWaitlistResponseDTOMapper.INSTANCE;

  @Override
  @Transactional
  public BookWaitlistResponseDTO joinWaitlist(User user, UUID bookId) {
    log.info("User {} joining waitlist for bookId={}", user.getId(), bookId);
    Book book = bookRepository.findById(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    if (waitlistRepository.existsByUserAndBook(user, book)) {
      // Idempotent-ish: a patron can waitlist a given title only once.
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Already on waitlist for this book");
    }

    BookWaitlist entry = BookWaitlist.builder()
        .user(user)
        .book(book)
        .build();
    BookWaitlist saved = waitlistRepository.save(entry);
    log.info("Waitlist entry created id={} for user {} / book {}", saved.getId(), user.getId(), bookId);
    return mapper.toResponseDTO(saved);
  }

  @Override
  @Transactional
  public List<BookWaitlistResponseDTO> getMyWaitlist(User user) {
    List<BookWaitlist> entries = waitlistRepository.findByUser(user);
    return mapper.toResponseDTOList(entries);
  }

  @Override
  @Transactional
  public void removeFromWaitlist(User user, UUID waitlistId) {
    BookWaitlist entry = waitlistRepository.findById(waitlistId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waitlist entry not found"));
    if (!entry.getUser().getId().equals(user.getId())) {
      // Patron can only remove their own entries; do not leak existence -> 404.
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Waitlist entry not found");
    }
    waitlistRepository.delete(entry);
    log.info("Removed waitlist entry id={} for user {}", waitlistId, user.getId());
  }
}
