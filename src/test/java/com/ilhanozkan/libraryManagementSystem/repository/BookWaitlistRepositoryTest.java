package com.ilhanozkan.libraryManagementSystem.repository;

import com.ilhanozkan.libraryManagementSystem.model.entity.Book;
import com.ilhanozkan.libraryManagementSystem.model.entity.BookWaitlist;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.enums.BookGenre;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserRole;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test for {@link BookWaitlistRepository} against H2 (test profile).
 * Mirrors the existing {@code @DataJpaTest} flavor used by BookRepositoryTest.
 */
@DataJpaTest
@ActiveProfiles("test")
class BookWaitlistRepositoryTest {

    @Autowired
    private BookWaitlistRepository waitlistRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindPendingEntriesByBook() {
        User patron = saveUser("alice");
        Book book = saveBook("9781111111111", "Dune");
        BookWaitlist entry = BookWaitlist.builder().user(patron).book(book).build();
        waitlistRepository.save(entry);

        List<BookWaitlist> pending = waitlistRepository.findByBookAndNotifiedAtIsNull(book);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getUser().getUsername()).isEqualTo("alice");
    }

    @Test
    void shouldExcludeAlreadyNotifiedEntries() {
        User patron = saveUser("bob");
        Book book = saveBook("9782222222222", "Hyperion");
        BookWaitlist notified = BookWaitlist.builder()
            .user(patron).book(book).notifiedAt(java.time.LocalDateTime.now()).build();
        waitlistRepository.save(notified);

        assertThat(waitlistRepository.findByBookAndNotifiedAtIsNull(book)).isEmpty();
        assertThat(waitlistRepository.findByBookIdAndNotifiedAtIsNull(book.getId())).isEmpty();
    }

    @Test
    void shouldLookupByBookIdWithoutLoadingEntity() {
        User patron = saveUser("carol");
        Book book = saveBook("9783333333333", "Foundation");
        waitlistRepository.save(BookWaitlist.builder().user(patron).book(book).build());

        List<BookWaitlist> pending = waitlistRepository.findByBookIdAndNotifiedAtIsNull(book.getId());
        assertThat(pending).hasSize(1);
    }

    @Test
    void shouldDetectExistingEntryForUserAndBook() {
        User patron = saveUser("dave");
        Book book = saveBook("9784444444444", "Neuromancer");
        waitlistRepository.save(BookWaitlist.builder().user(patron).book(book).build());

        assertThat(waitlistRepository.existsByUserAndBook(patron, book)).isTrue();
        assertThat(waitlistRepository.findByUserAndBook(patron, book)).isPresent();
    }

    @Test
    void shouldListAllEntriesForUser() {
        User patron = saveUser("eve");
        Book b1 = saveBook("9785555555555", "Snow Crash");
        Book b2 = saveBook("9786666666666", "Cryptonomicon");
        waitlistRepository.save(BookWaitlist.builder().user(patron).book(b1).build());
        waitlistRepository.save(BookWaitlist.builder().user(patron).book(b2).notifiedAt(java.time.LocalDateTime.now()).build());

        List<BookWaitlist> mine = waitlistRepository.findByUser(patron);
        assertThat(mine).hasSize(2);
    }

    // --- helpers ---

    private User saveUser(String username) {
        return userRepository.save(User.builder()
            .username(username).email(username + "@example.com").password("password")
            .name(username).surname("Last").role(UserRole.PATRON).status(UserStatus.ACTIVE)
            .build());
    }

    private Book saveBook(String isbn, String name) {
        return bookRepository.save(Book.builder()
            .isbn(isbn).name(name).author("A").publisher("P")
            .numberOfPages(1).quantity(1).availableQuantity(1).genre(BookGenre.OTHER)
            .build());
    }
}
