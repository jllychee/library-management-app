package com.ilhanozkan.libraryManagementSystem.service.impl;

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
import com.ilhanozkan.libraryManagementSystem.model.enums.BookGenre;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserRole;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserStatus;
import com.ilhanozkan.libraryManagementSystem.repository.BookRepository;
import com.ilhanozkan.libraryManagementSystem.repository.BorrowingRepository;
import com.ilhanozkan.libraryManagementSystem.repository.UserRepository;
import com.ilhanozkan.libraryManagementSystem.service.BookService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BorrowingServiceImplTest {

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookService bookService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BorrowingServiceImpl borrowingService;

    private User testUser;
    private Book testBook;
    private Borrowing testBorrowing;
    private UUID userId;
    private UUID bookId;
    private UUID borrowingId;
    private BorrowingRequestDTO borrowingRequestDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        borrowingId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .name("Test")
                .surname("User")
                .role(UserRole.PATRON)
                .status(UserStatus.ACTIVE)
                .build();

        testBook = Book.builder()
                .id(bookId)
                .name("Test Book")
                .isbn("9781234567890")
                .author("Test Author")
                .publisher("Test Publisher")
                .numberOfPages(200)
                .quantity(10)
                .availableQuantity(10)
                .genre(BookGenre.SCIENCE_FICTION)
                .build();

        LocalDateTime now = LocalDateTime.now();
        testBorrowing = Borrowing.builder()
                .id(borrowingId)
                .book(testBook)
                .user(testUser)
                .borrowDate(now)
                .dueDate(now.plusDays(14))
                .returned(false)
                .updatedAt(now)
                .build();

        borrowingRequestDTO = new BorrowingRequestDTO(
            bookId,
            userId
        );

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldGetAllBorrowings() {
        // Arrange
        List<Borrowing> borrowings = Arrays.asList(testBorrowing);
        given(borrowingRepository.findAll()).willReturn(borrowings);

        // Act
        List<BorrowingResponseDTO> result = borrowingService.getBorrowings();

        // Assert
        assertThat(result).hasSize(1);
        verify(borrowingRepository, times(1)).findAll();
    }

    @Test
    void shouldGetBorrowingsByUserId() {
        // Arrange
        List<Borrowing> borrowings = Arrays.asList(testBorrowing);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(borrowingRepository.findByUser(testUser)).willReturn(borrowings);

        // Act
        List<BorrowingResponseDTO> result = borrowingService.getBorrowingsByUserId(userId);

        // Assert
        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findById(userId);
        verify(borrowingRepository, times(1)).findByUser(testUser);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundForBorrowings() {
        // Arrange
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> borrowingService.getBorrowingsByUserId(userId));
        verify(userRepository, times(1)).findById(userId);
        verify(borrowingRepository, never()).findByUser(any(User.class));
    }

    @Test
    void shouldGetActiveBorrowingsByUserId() {
        // Arrange
        List<Borrowing> activeBorrowings = Arrays.asList(testBorrowing);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(borrowingRepository.findByUserAndReturnedFalse(testUser)).willReturn(activeBorrowings);

        // Act
        List<BorrowingResponseDTO> result = borrowingService.getActiveBorrowingsByUserId(userId);

        // Assert
        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findById(userId);
        verify(borrowingRepository, times(1)).findByUserAndReturnedFalse(testUser);
    }

    @Test
    void shouldCreateBorrowing() {
        // Arrange
        given(bookRepository.findById(bookId)).willReturn(Optional.of(testBook));
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(borrowingRepository.save(any(Borrowing.class))).willReturn(testBorrowing);
        doNothing().when(bookService).publishBookAvailabilityEvent(any(Book.class), anyInt());

        // Act
        BorrowingResponseDTO result = borrowingService.createBorrowing(borrowingRequestDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(bookRepository, times(1)).findById(bookId);
        verify(userRepository, times(1)).findById(userId);
        verify(borrowingRepository, times(1)).save(any(Borrowing.class));
        verify(bookService, times(1)).publishBookAvailabilityEvent(any(Book.class), anyInt());
    }

    @Test
    void shouldThrowExceptionWhenBookNotFoundForBorrowing() {
        // Arrange
        given(bookRepository.findById(bookId)).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(BookNotFoundException.class, () -> borrowingService.createBorrowing(borrowingRequestDTO));
        verify(bookRepository, times(1)).findById(bookId);
        verify(userRepository, never()).findById(any(UUID.class));
        verify(borrowingRepository, never()).save(any(Borrowing.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundForBorrowing() {
        // Arrange
        given(bookRepository.findById(bookId)).willReturn(Optional.of(testBook));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> borrowingService.createBorrowing(borrowingRequestDTO));
        verify(bookRepository, times(1)).findById(bookId);
        verify(userRepository, times(1)).findById(userId);
        verify(borrowingRepository, never()).save(any(Borrowing.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotActiveForBorrowing() {
        // Arrange
        testUser.setStatus(UserStatus.INACTIVE);
        given(bookRepository.findById(bookId)).willReturn(Optional.of(testBook));
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(UserIsNotActiveException.class, () -> borrowingService.createBorrowing(borrowingRequestDTO));
        verify(bookRepository, times(1)).findById(bookId);
        verify(userRepository, times(1)).findById(userId);
        verify(borrowingRepository, never()).save(any(Borrowing.class));
    }

    @Test
    void shouldThrowExceptionWhenBookNotAvailableForBorrowing() {
        // Arrange
        testBook.setAvailableQuantity(0);
        given(bookRepository.findById(bookId)).willReturn(Optional.of(testBook));
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(BookNotAvailableException.class, () -> borrowingService.createBorrowing(borrowingRequestDTO));
        verify(bookRepository, times(1)).findById(bookId);
        verify(userRepository, times(1)).findById(userId);
        verify(borrowingRepository, never()).save(any(Borrowing.class));
    }

    @Test
    void shouldReturnBook() {
        // Arrange
        given(borrowingRepository.findById(borrowingId)).willReturn(Optional.of(testBorrowing));
        
        // Mock authentication
        UserPrincipal userPrincipal = new UserPrincipal(testUser);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userPrincipal);
        
        given(borrowingRepository.save(any(Borrowing.class))).willReturn(testBorrowing);
        doNothing().when(bookService).publishBookAvailabilityEvent(any(Book.class), anyInt());

        // Act
        BorrowingResponseDTO result = borrowingService.returnBook(borrowingId);

        // Assert
        assertThat(result).isNotNull();
        // Verify book's available quantity is incremented
        assertThat(testBook.getAvailableQuantity()).isEqualTo(11); // Should be incremented from 10 to 11
        verify(borrowingRepository, times(1)).findById(borrowingId);
        verify(borrowingRepository, times(1)).save(any(Borrowing.class));
        verify(bookRepository, times(1)).save(any(Book.class));
        verify(bookService, times(1)).publishBookAvailabilityEvent(any(Book.class), anyInt());
    }

    @Test
    void shouldDeleteBorrowing() {
        // Arrange
        given(borrowingRepository.findById(borrowingId)).willReturn(Optional.of(testBorrowing));
        doNothing().when(bookService).publishBookAvailabilityEvent(any(Book.class), anyInt());
        doNothing().when(borrowingRepository).delete(any(Borrowing.class));

        // Act
        borrowingService.deleteBorrowing(borrowingId);

        // Assert
        // Verify book's available quantity is incremented
        assertThat(testBook.getAvailableQuantity()).isEqualTo(11); // Should be incremented from 10 to 11
        verify(borrowingRepository, times(1)).findById(borrowingId);
        verify(bookRepository, times(1)).save(any(Book.class));
        verify(bookService, times(1)).publishBookAvailabilityEvent(any(Book.class), anyInt());
        verify(borrowingRepository, times(1)).delete(any(Borrowing.class));
    }

    @Test
    void shouldThrowExceptionWhenBorrowingNotFoundForDelete() {
        // Arrange
        given(borrowingRepository.findById(borrowingId)).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(BorrowingNotFoundException.class, () -> borrowingService.deleteBorrowing(borrowingId));
        verify(borrowingRepository, times(1)).findById(borrowingId);
        verify(bookService, never()).updateBookQuantity(any(UUID.class), anyInt());
        verify(borrowingRepository, never()).delete(any(Borrowing.class));
    }
}