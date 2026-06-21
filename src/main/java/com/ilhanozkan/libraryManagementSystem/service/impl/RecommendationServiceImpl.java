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
import com.ilhanozkan.libraryManagementSystem.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

  private final BorrowingRepository borrowingRepository;
  private final BookRepository bookRepository;
  private final UserRepository userRepository;
  private final BookResponseDTOMapper bookResponseDTOMapper;

  @Override
  public List<BookResponseDTO> recommendBooks(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    List<Borrowing> history = borrowingRepository.findByUser(user);

    if (history.isEmpty()) {
      List<Book> books = bookRepository.findAll()
          .stream()
          .filter(book -> book.getAvailableQuantity() > 0)
          .limit(5)
          .toList();
      return bookResponseDTOMapper.toBookResponseDTOList(books);
    }

    List<BookGenre> preferredGenres = history.stream()
        .map(borrowing -> borrowing.getBook().getGenre())
        .distinct()
        .toList();

    List<String> preferredAuthors = history.stream()
        .map(borrowing -> borrowing.getBook().getAuthor())
        .distinct()
        .toList();

    Set<UUID> borrowedBookIds = history.stream()
        .map(borrowing -> borrowing.getBook().getId())
        .collect(Collectors.toSet());

    List<Book> books = bookRepository.findByGenreInOrAuthorIn(preferredGenres, preferredAuthors)
        .stream()
        .filter(book -> !borrowedBookIds.contains(book.getId()))
        .filter(book -> book.getAvailableQuantity() > 0)
        .limit(5)
        .toList();
    return bookResponseDTOMapper.toBookResponseDTOList(books);
  }
}
