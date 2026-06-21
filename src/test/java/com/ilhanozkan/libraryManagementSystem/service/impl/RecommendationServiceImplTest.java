package com.ilhanozkan.libraryManagementSystem.service.impl;

import com.ilhanozkan.libraryManagementSystem.common.exception.user.UserNotFoundException;
import com.ilhanozkan.libraryManagementSystem.model.dto.response.BookResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.Book;
import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.enums.BookGenre;
import com.ilhanozkan.libraryManagementSystem.model.mapper.BookResponseDTOMapper;
import com.ilhanozkan.libraryManagementSystem.repository.BookRepository;
import com.ilhanozkan.libraryManagementSystem.repository.BorrowingRepository;
import com.ilhanozkan.libraryManagementSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {
  @Mock
  private BorrowingRepository borrowingRepository;
  @Mock
  private BookRepository bookRepository;
  @Mock
  private UserRepository userRepository;

  private RecommendationServiceImpl recommendationService;
  private User user;

  @BeforeEach
  void setUp() {
    recommendationService = new RecommendationServiceImpl(
        borrowingRepository,
        bookRepository,
        userRepository,
        BookResponseDTOMapper.INSTANCE
    );
    user = User.builder().id(UUID.randomUUID()).username("patron").build();
  }

  @Test
  void shouldRecommendAvailableUnborrowedBooksFromPreferredGenreOrAuthor() {
    Book borrowedBook = book("Borrowed", "Author A", BookGenre.FANTASY, 1);
    Book matchingBook = book("Recommended", "Author B", BookGenre.FANTASY, 2);
    Book unavailableBook = book("Unavailable", "Author A", BookGenre.OTHER, 0);

    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    given(borrowingRepository.findByUser(user)).willReturn(List.of(
        Borrowing.builder().user(user).book(borrowedBook).build()
    ));
    given(bookRepository.findByGenreInOrAuthorIn(
        List.of(BookGenre.FANTASY),
        List.of("Author A")
    )).willReturn(List.of(borrowedBook, matchingBook, unavailableBook));

    List<BookResponseDTO> recommendations = recommendationService.recommendBooks(user.getId());

    assertThat(recommendations)
        .extracting(BookResponseDTO::name)
        .containsExactly("Recommended");
  }

  @Test
  void shouldReturnAvailableBooksWhenUserHasNoHistory() {
    Book availableBook = book("Available", "Author A", BookGenre.FANTASY, 1);
    Book unavailableBook = book("Unavailable", "Author B", BookGenre.OTHER, 0);

    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    given(borrowingRepository.findByUser(user)).willReturn(List.of());
    given(bookRepository.findAll()).willReturn(List.of(unavailableBook, availableBook));

    List<BookResponseDTO> recommendations = recommendationService.recommendBooks(user.getId());

    assertThat(recommendations)
        .extracting(BookResponseDTO::name)
        .containsExactly("Available");
  }

  @Test
  void shouldThrowWhenUserDoesNotExist() {
    UUID userId = UUID.randomUUID();
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> recommendationService.recommendBooks(userId));
  }

  private Book book(String name, String author, BookGenre genre, int availableQuantity) {
    return Book.builder()
        .id(UUID.randomUUID())
        .name(name)
        .isbn("9781234567890")
        .author(author)
        .publisher("Publisher")
        .numberOfPages(100)
        .quantity(2)
        .availableQuantity(availableQuantity)
        .genre(genre)
        .build();
  }
}
