package com.ilhanozkan.libraryManagementSystem.service.impl;

import com.ilhanozkan.libraryManagementSystem.common.exception.book.BookAlreadyReturnedException;
import com.ilhanozkan.libraryManagementSystem.common.exception.book.BookNotAvailableException;
import com.ilhanozkan.libraryManagementSystem.common.exception.book.BookNotFoundException;
import com.ilhanozkan.libraryManagementSystem.common.exception.borrowing.BorrowingNotFoundException;
import com.ilhanozkan.libraryManagementSystem.common.exception.user.UserIsNotActiveException;
import com.ilhanozkan.libraryManagementSystem.common.exception.user.UserNotFoundException;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.BorrowingRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.response.BorrowingResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.Book;
import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.entity.UserPrincipal;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserStatus;
import com.ilhanozkan.libraryManagementSystem.model.mapper.BorrowingResponseDTOMapper;
import com.ilhanozkan.libraryManagementSystem.repository.BookRepository;
import com.ilhanozkan.libraryManagementSystem.repository.BorrowingRepository;
import com.ilhanozkan.libraryManagementSystem.repository.UserRepository;
import com.ilhanozkan.libraryManagementSystem.service.BookService;
import com.ilhanozkan.libraryManagementSystem.service.BorrowingService;
import com.ilhanozkan.libraryManagementSystem.util.CustomDateTimeFormatter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowingServiceImpl implements BorrowingService {
  private final BorrowingRepository borrowingRepository;
  private final BookRepository bookRepository;
  private final UserRepository userRepository;
  private final BookService bookService;
  private final BorrowingResponseDTOMapper mapper = BorrowingResponseDTOMapper.INSTANCE;

  @Transactional
  public List<BorrowingResponseDTO> getBorrowings() {
    log.debug("Fetching all borrowings");
    try {
      List<Borrowing> borrowings = borrowingRepository.findAll();
      log.debug("Retrieved {} borrowings", borrowings.size());
      return mapper.toBorrowingResponseDTOList(borrowings);
    } catch (RuntimeException e) {
      log.error("Error retrieving all borrowings", e);
      throw new RuntimeException(e.getMessage());
    }
  }

  @Transactional
  public List<BorrowingResponseDTO> getBorrowingsByUserId(UUID userId) {
    log.debug("Fetching borrowings for user ID: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("User not found with ID: {}", userId);
          return new UserNotFoundException(userId);
        });

    List<Borrowing> borrowings = borrowingRepository.findByUser(user);
    log.debug("Retrieved {} borrowings for user ID: {}", borrowings.size(), userId);
    return mapper.toBorrowingResponseDTOList(borrowings);
  }

  @Transactional
  public List<BorrowingResponseDTO> getActiveBorrowingsByUserId(UUID userId) {
    log.debug("Fetching active borrowings for user ID: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("User not found with ID: {}", userId);
          return new UserNotFoundException(userId);
        });

    List<Borrowing> activeBorrowings = borrowingRepository.findByUserAndReturnedFalse(user);
    log.debug("Retrieved {} active borrowings for user ID: {}", activeBorrowings.size(), userId);
    return mapper.toBorrowingResponseDTOList(activeBorrowings);
  }

  @Transactional
  public BorrowingResponseDTO createBorrowing(BorrowingRequestDTO borrowingRequestDTO) {
      log.info("Creating new borrowing - Book ID: {}, User ID: {}", 
               borrowingRequestDTO.getBookId(), borrowingRequestDTO.getUserId());
      
      Book book = bookRepository.findById(borrowingRequestDTO.getBookId()).orElseThrow(
          () -> {
            log.warn("Book not found with ID: {}", borrowingRequestDTO.getBookId());
            return new BookNotFoundException(borrowingRequestDTO.getBookId());
          }
      );

      User user = userRepository.findById(borrowingRequestDTO.getUserId()).orElseThrow(
          () -> {
            log.warn("User not found with ID: {}", borrowingRequestDTO.getUserId());
            return new UserNotFoundException(borrowingRequestDTO.getUserId());
          }
      );

      if (user.getStatus() != UserStatus.ACTIVE) {
        log.warn("User with ID {} is not active. Current status: {}", 
                 borrowingRequestDTO.getUserId(), user.getStatus());
        throw new UserIsNotActiveException(borrowingRequestDTO.getUserId());
      }

      if (book.getAvailableQuantity() <= 0) {
        log.warn("Book with ID {} is not available. Current available quantity: {}", 
                 borrowingRequestDTO.getBookId(), book.getAvailableQuantity());
        throw new BookNotAvailableException(borrowingRequestDTO.getBookId());
      }

      // Update the available quantity directly
      book.setAvailableQuantity(book.getAvailableQuantity() - 1);
      bookRepository.save(book);

      Borrowing borrowing = new Borrowing();
      borrowing.setBorrowDate(LocalDateTime.now());
      borrowing.setDueDate(LocalDateTime.now().plusDays(14));
      borrowing.setBook(book);
      borrowing.setUser(user);
      borrowing.setReturned(false);
      borrowing.setUpdatedAt(LocalDateTime.now());

      Borrowing savedBorrowing = borrowingRepository.save(borrowing);

      // Stream book availability event after the direct update.
      // previous = current + 1 (the value before decrement); a decrease can never be a 0->>0 transition.
      bookService.publishBookAvailabilityEvent(book, book.getAvailableQuantity() + 1);

      log.info("Borrowing created successfully with ID: {}", savedBorrowing.getId());
      return mapper.toBorrowingResponseDTO(savedBorrowing);
  }

  @Transactional
  public BorrowingResponseDTO returnBook(UUID id) {
      log.info("Processing book return for borrowing ID: {}", id);
      Borrowing borrowing = borrowingRepository.findById(id).orElseThrow(
          () -> {
            log.warn("Borrowing not found with ID: {}", id);
            return new BorrowingNotFoundException(id);
          }
      );

      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
      User user = userPrincipal.getUser();

      // If the user is not the borrower or user role is not librarian, return an error
      if (!borrowing.getUser().getId().equals(user.getId()) && !userPrincipal.hasRole("LIBRARIAN")) {
        log.warn("User with ID {} is not authorized to return this book. Borrower ID: {}",
                 user.getId(), borrowing.getUser().getId());
        throw new AccessDeniedException("User is not authorized to return this book");
      }

      if (borrowing.getReturned()) {
        log.warn("Book already returned for borrowing ID: {}", id);
        throw new BookAlreadyReturnedException(id);
      }

      borrowing.setReturned(true);
      borrowing.setReturnDate(LocalDateTime.now());
      
      // Update the book's available quantity directly
      Book book = borrowing.getBook();
      int previousAvailableQuantity = book.getAvailableQuantity();
      book.setAvailableQuantity(book.getAvailableQuantity() + 1);
      bookRepository.save(book);

      Borrowing savedBorrowing = borrowingRepository.save(borrowing);

      // Stream book availability event. previous = the pre-increment value, so a
      // return that bumps the book from 0 -> 1 correctly triggers waitlist notifications.
      bookService.publishBookAvailabilityEvent(borrowing.getBook(), previousAvailableQuantity);

      log.info("Book returned successfully for borrowing ID: {}", id);
      return mapper.toBorrowingResponseDTO(savedBorrowing);
  }

  @Transactional
  public void deleteBorrowing(UUID id) {
    log.info("Deleting borrowing with ID: {}", id);
    Borrowing borrowing = borrowingRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Borrowing not found with ID: {}", id);
          return new BorrowingNotFoundException(id);
        });

    if (!borrowing.getReturned()) {
      log.debug("Book was not returned, updating available quantity for book ID: {}", 
                borrowing.getBook().getId());
      // Update the book's available quantity directly
      Book book = borrowing.getBook();
      int previousAvailableQuantity = book.getAvailableQuantity();
      book.setAvailableQuantity(book.getAvailableQuantity() + 1);
      bookRepository.save(book);
      
      // Also publish the event
      bookService.publishBookAvailabilityEvent(book, previousAvailableQuantity);
    }

    borrowingRepository.delete(borrowing);
    log.info("Borrowing deleted successfully: {}", id);
  }

  @Transactional
  public byte[] getOverdueBooksPDFReport() {
    log.info("Generating overdue books PDF report");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try {
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdf = new PdfDocument(writer);
      Document document = new Document(pdf);

      document.add(new Paragraph(getOverdueBooksTextReport()));
      document.close();

      byte[] pdfBytes = baos.toByteArray();
      log.info("Overdue books PDF report generated successfully, size: {} bytes", pdfBytes.length);
      return pdfBytes;
    } catch (Exception e) {
      log.error("Error generating overdue books PDF report", e);
      throw new RuntimeException("Failed to generate PDF report: " + e.getMessage());
    }
  }

  @Transactional
  public String getOverdueBooksTextReport() {
    log.info("Generating overdue books text report");

    StringBuilder report = new StringBuilder();
    report.append("Overdue Books Report\n");
    report.append("Report Generation Date: ").append(CustomDateTimeFormatter.formatDateTime(java.time.LocalDateTime.now())).append("\n\n");

    List<Borrowing> overdueBooks = borrowingRepository.findOverdueBooks();
    long totalBorrowings = borrowingRepository.count();
    
    log.debug("Total borrowings: {}, Overdue books: {}", totalBorrowings, overdueBooks.size());

    report.append("Total Borrowings Count: ").append(totalBorrowings).append("\n");
    report.append("Total Overdue Books Count: ").append(overdueBooks.size()).append("\n\n");

    report.append("Overdue Books:\n");
    report.append("-------------------------\n");
    for (Borrowing borrowing : overdueBooks) {
      report.append("Book ID: ").append(borrowing.getBook().getId()).append("\n");
      report.append("Book Name: ").append(borrowing.getBook().getName()).append("\n");
      report.append("Book Author: ").append(borrowing.getBook().getAuthor()).append("\n");
      report.append("Borrow Date: ").append(CustomDateTimeFormatter.formatDateTime(borrowing.getBorrowDate())).append("\n");

      // Using CustomDateTimeFormatter to format the due date
      report.append("Due Date: ").append(CustomDateTimeFormatter.formatDateTime(borrowing.getDueDate())).append("\n");
      report.append("User ID: ").append(borrowing.getUser().getId()).append("\n");
      report.append("User Name: ").append(borrowing.getUser().getName()).append("\n");
      report.append("User Surname: ").append(borrowing.getUser().getSurname()).append("\n");
      report.append("User Email: ").append(borrowing.getUser().getEmail()).append("\n");
      report.append("-------------------------\n");
    }

    log.info("Overdue books text report generated successfully");
    return report.toString();
  }
}
