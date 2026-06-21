package com.ilhanozkan.libraryManagementSystem.service.impl;

import com.ilhanozkan.libraryManagementSystem.common.exception.book.BookNotFoundException;
import com.ilhanozkan.libraryManagementSystem.common.exception.book.NotEnoughBooksAvailableException;
import com.ilhanozkan.libraryManagementSystem.model.dto.event.BookAvailabilityEvent;
import com.ilhanozkan.libraryManagementSystem.model.dto.event.BookBecameAvailableEvent;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.BookQuantityUpdateDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.BookRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.response.BookResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.response.BorrowingResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.response.PagedResponse;
import com.ilhanozkan.libraryManagementSystem.model.entity.Book;
import com.ilhanozkan.libraryManagementSystem.model.enums.BookGenre;
import com.ilhanozkan.libraryManagementSystem.model.mapper.BookResponseDTOMapper;
import com.ilhanozkan.libraryManagementSystem.repository.BookRepository;
import com.ilhanozkan.libraryManagementSystem.service.BookAvailabilityPublisher;
import com.ilhanozkan.libraryManagementSystem.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BookServiceImpl implements BookService {
  private final BookRepository bookRepository;
  private final BookResponseDTOMapper mapper = BookResponseDTOMapper.INSTANCE;
  private final BookAvailabilityPublisher bookAvailabilityPublisher;
  private final ApplicationEventPublisher eventPublisher;

  @Autowired
  public BookServiceImpl(BookRepository bookRepository,
                         BookAvailabilityPublisher bookAvailabilityPublisher,
                         ApplicationEventPublisher eventPublisher) {
    this.bookRepository = bookRepository;
    this.bookAvailabilityPublisher = bookAvailabilityPublisher;
    this.eventPublisher = eventPublisher;
    log.info("BookServiceImpl initialized");
  }

  @Override
  public void publishBookAvailabilityEvent(Book book, int previousAvailableQuantity) {
    log.debug("Publishing book availability event for book ID: {} (was={}, now={})",
        book.getId(), previousAvailableQuantity, book.getAvailableQuantity());

    // 1) Reactive SSE stream (existing behavior).
    BookAvailabilityEvent event = BookAvailabilityEvent.create(
        book.getId(),
        book.getName(),
        book.getIsbn(),
        book.getQuantity(),
        book.getAvailableQuantity()
    );
    bookAvailabilityPublisher.publishEvent(event);

    // 2) Transition 0 -> >0: notify waitlisted patrons via Spring application event
    //    (handled AFTER_COMMIT + @Async by BookAvailabilityNotificationListener).
    if (previousAvailableQuantity <= 0 && book.getAvailableQuantity() > 0) {
      log.info("Book {} became available (0 -> {}); publishing BookBecameAvailableEvent",
          book.getId(), book.getAvailableQuantity());
      eventPublisher.publishEvent(new BookBecameAvailableEvent(
          book.getId(), book.getName(), book.getIsbn(), book.getAvailableQuantity()));
    }
  }

  public PagedResponse<BookResponseDTO> getAllBooks(Pageable pageable) {
    log.debug("Getting all books with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
    Page<Book> booksPage = bookRepository.findAll(pageable);
    List<Book> bookResponses = booksPage.getContent();

    log.debug("Retrieved {} books out of {} total", bookResponses.size(), booksPage.getTotalElements());
    return PagedResponse.<BookResponseDTO>builder()
        .content(mapper.toBookResponseDTOList(bookResponses))
        .page(booksPage.getNumber())
        .size(booksPage.getSize())
        .totalElements(booksPage.getTotalElements())
        .totalPages(booksPage.getTotalPages())
        .last(booksPage.isLast())
        .build();
  }

  // Search books by title, author, ISBN, or genre
  public PagedResponse<BookResponseDTO> searchBooks(String title, String author, String isbn, String genre, Pageable pageable) {
    log.debug("Searching books with parameters - title: {}, author: {}, isbn: {}, genre: {}", title, author, isbn, genre);
    // If all parameters are null, return all books
    if (title == null && author == null && isbn == null && genre == null) {
      Page<Book> booksPage = bookRepository.findAll(pageable);
      List<Book> bookResponses = booksPage.getContent();

      log.debug("No search parameters provided, returning all books");
      return PagedResponse.<BookResponseDTO>builder()
          .content(mapper.toBookResponseDTOList(bookResponses))
          .page(booksPage.getNumber())
          .size(booksPage.getSize())
          .totalElements(booksPage.getTotalElements())
          .totalPages(booksPage.getTotalPages())
          .last(booksPage.isLast())
          .build();
    }
    
    // Convert genre string to enum ordinal value if provided
    String genreValue = null;
    if (genre != null && !genre.trim().isEmpty()) {
      try {
        BookGenre genreEnum = BookGenre.valueOf(genre.toUpperCase());
        genreValue = String.valueOf(genreEnum.ordinal());
        log.debug("Converted genre '{}' to ordinal value '{}'", genre, genreValue);
      } catch (IllegalArgumentException e) {
        // Invalid genre provided, will return empty list
        log.warn("Invalid genre provided: {}", genre);
        return PagedResponse.<BookResponseDTO>builder()
            .content(List.of())
            .page(0)
            .size(0)
            .totalElements(0)
            .totalPages(0)
            .last(true)
            .build();
      }
    }
    
    List<Book> books = bookRepository.searchBooks(title, author, isbn, genreValue, pageable);
    log.debug("Found {} books matching the search criteria", books.size());
    return PagedResponse.<BookResponseDTO>builder()
        .content(mapper.toBookResponseDTOList(books))
        .page(pageable.getPageNumber())
        .size(pageable.getPageSize())
        .totalElements(books.size())
        .totalPages((int) Math.ceil((double) books.size() / pageable.getPageSize()))
        .last(books.size() < pageable.getPageSize())
        .build();
  }

  private Book findBookById(UUID id) {
    log.debug("Looking up book with ID: {}", id);
    Book book = bookRepository.findById(id).orElseThrow(() -> {
      log.warn("Book not found with ID: {}", id);
      return new BookNotFoundException(id);
    });
    log.debug("Found book: {}", book.getName());
    return book;
  }

  public BookResponseDTO getBookById(UUID id) {
    log.debug("Getting book by ID: {}", id);
    return mapper.toBookResponseDTO(findBookById(id));
  }
  
  public BookResponseDTO getBookByIsbn(String isbn) {
    log.debug("Getting book by ISBN: {}", isbn);
    Book book = bookRepository.findByIsbn(isbn);
    if (book == null) {
        log.warn("Book not found with ISBN: {}", isbn);
        throw new BookNotFoundException("Book with ISBN " + isbn + " not found");
    }
    log.debug("Found book with ISBN {}: {}", isbn, book.getName());
    return mapper.toBookResponseDTO(book);
  }

  public BookResponseDTO createBook(BookRequestDTO bookRequestDTO) {
    log.info("Creating new book: {} by {}", bookRequestDTO.getName(), bookRequestDTO.getAuthor());
    Book newBook = Book.builder()
        .isbn(bookRequestDTO.getIsbn())
        .name(bookRequestDTO.getName())
        .author(bookRequestDTO.getAuthor())
        .publisher(bookRequestDTO.getPublisher())
        .quantity(bookRequestDTO.getQuantity())
        .availableQuantity(bookRequestDTO.getQuantity())
        .numberOfPages(bookRequestDTO.getNumberOfPages())
        .genre(bookRequestDTO.getGenre())
        .build();

    Book savedBook = bookRepository.save(newBook);
    log.info("Book created successfully with ID: {}", savedBook.getId());
    
    // Publish event for the new book. Brand-new books have no waitlisters; passing the
    // current quantity means previous == current so no 0->>0 transition alert fires.
    publishBookAvailabilityEvent(savedBook, savedBook.getAvailableQuantity());
    
    return mapper.toBookResponseDTO(savedBook);
  }

  @Transactional
  public BookResponseDTO updateBook(UUID id, BookRequestDTO bookRequestDTO) {
    log.info("Updating book with ID: {}", id);
    Book updatedBook = findBookById(id);
    
    // Save old values to check if availability changes
    int oldQuantity = updatedBook.getQuantity();
    int oldAvailableQuantity = updatedBook.getAvailableQuantity();
    
    log.debug("Updating book details - old name: {}, new name: {}", updatedBook.getName(), bookRequestDTO.getName());
    updatedBook.setIsbn(bookRequestDTO.getIsbn());
    updatedBook.setName(bookRequestDTO.getName());
    updatedBook.setAuthor(bookRequestDTO.getAuthor());
    updatedBook.setPublisher(bookRequestDTO.getPublisher());
    updatedBook.setQuantity(bookRequestDTO.getQuantity());
    updatedBook.setNumberOfPages(bookRequestDTO.getNumberOfPages());
    updatedBook.setGenre(bookRequestDTO.getGenre());

    Book savedBook = bookRepository.save(updatedBook);
    log.info("Book updated successfully: {}", savedBook.getName());
    
    // Publish event if availability changed
    if (oldQuantity != savedBook.getQuantity() || oldAvailableQuantity != savedBook.getAvailableQuantity())
        publishBookAvailabilityEvent(savedBook, oldAvailableQuantity);
    
    return mapper.toBookResponseDTO(savedBook);
  }

  @Transactional
  public void updateBookQuantity(UUID id, int change) {
    log.debug("Updating book quantity - ID: {}, change: {}", id, change);
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Book not found with ID: {}", id);
          return new BookNotFoundException(id);
        });

    int currentQuantity = book.getAvailableQuantity();
    int newQuantity = currentQuantity + change;
    log.debug("Book: {}, current quantity: {}, new quantity: {}", book.getName(), currentQuantity, newQuantity);

    if (newQuantity < 0) {
      log.warn("Not enough books available for book with ID: {}", id);
      throw new NotEnoughBooksAvailableException();
    }

    book.setAvailableQuantity(newQuantity);
    Book savedBook = bookRepository.save(book);
    log.info("Book quantity updated successfully - ID: {}, new quantity: {}", id, newQuantity);
    
    // Publish event for quantity change
    publishBookAvailabilityEvent(savedBook, currentQuantity);
  }

  @Transactional
  public BookResponseDTO updateBookAvailableQuantity(UUID id, BookQuantityUpdateDTO quantityUpdateDTO) {
    log.info("Updating book available quantity - ID: {}, new quantity: {}", id, quantityUpdateDTO.getAvailableQuantity());
    Book book = findBookById(id);
    
    // Validate that the available quantity is not greater than total quantity
    if (quantityUpdateDTO.getAvailableQuantity() > book.getQuantity()) {
      log.warn("Available quantity ({}) exceeds total quantity ({}) for book ID: {}", 
               quantityUpdateDTO.getAvailableQuantity(), book.getQuantity(), id);
      throw new IllegalArgumentException("Available quantity cannot exceed total quantity");
    }
    
    // Validate that the available quantity is not negative
    if (quantityUpdateDTO.getAvailableQuantity() < 0) {
      log.warn("Negative available quantity ({}) provided for book ID: {}", 
               quantityUpdateDTO.getAvailableQuantity(), id);
      throw new IllegalArgumentException("Available quantity cannot be negative");
    }
    
    // Check if availability is changing
    int previousAvailableQuantity = book.getAvailableQuantity();
    boolean availabilityChanged = previousAvailableQuantity != quantityUpdateDTO.getAvailableQuantity();
    
    book.setAvailableQuantity(quantityUpdateDTO.getAvailableQuantity());
    Book savedBook = bookRepository.save(book);
    log.info("Book available quantity updated successfully - ID: {}, new quantity: {}", id, savedBook.getAvailableQuantity());
    
    // Publish event if availability changed
    if (availabilityChanged) {
        publishBookAvailabilityEvent(savedBook, previousAvailableQuantity);
    }
    
    return mapper.toBookResponseDTO(savedBook);
  }

  @Transactional
  public void deleteBook(UUID id) {
    log.info("Deleting book with ID: {}", id);
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> {
          log.warn("Cannot delete: Book not found with ID: {}", id);
          return new BookNotFoundException(id);
        });

    log.debug("Found book to delete: {}", book.getName());
    bookRepository.deleteById(id);
    log.info("Book deleted successfully: {}", book.getName());
  }
}
