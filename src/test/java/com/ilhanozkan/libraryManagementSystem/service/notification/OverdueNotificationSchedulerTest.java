package com.ilhanozkan.libraryManagementSystem.service.notification;

import com.ilhanozkan.libraryManagementSystem.model.entity.Book;
import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.enums.BookGenre;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserRole;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserStatus;
import com.ilhanozkan.libraryManagementSystem.repository.BorrowingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pure unit test for {@link OverdueNotificationScheduler}. Verifies grouping by user,
 * per-user email dispatch, and that lastOverdueNotifiedAt is stamped on every borrowing.
 */
@ExtendWith(MockitoExtension.class)
class OverdueNotificationSchedulerTest {

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private EmailNotificationService emailService;

    @InjectMocks
    private OverdueNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "remindDays", 3L);
    }

    @Test
    void shouldGroupByUserAndStampEachBorrowing() {
        User alice = user("alice", UUID.randomUUID());
        User bob = user("bob", UUID.randomUUID());

        Borrowing aliceOverdue = overdue(alice, "Alice Book", 11);
        Borrowing bobOverdue1 = overdue(bob, "Bob Book 1", 22);
        Borrowing bobOverdue2 = overdue(bob, "Bob Book 2", 33);

        when(borrowingRepository.findOverdueBooksNeedingReminder(any(LocalDateTime.class)))
            .thenReturn(List.of(aliceOverdue, bobOverdue1, bobOverdue2));

        scheduler.sendOverdueNotifications();

        // One digest email per patron (2 distinct users).
        verify(emailService).sendOverdueWarning(eq(alice), anyList());
        verify(emailService).sendOverdueWarning(eq(bob), anyList());
        verifyNoMoreInteractions(emailService);

        // Every borrowing is stamped + saved exactly once.
        verify(borrowingRepository, times(3)).save(any(Borrowing.class));
    }

    @Test
    void shouldStampNotifiedAtTimestampOnBorrowings() {
        User alice = user("alice", UUID.randomUUID());
        Borrowing b = overdue(alice, "Alice Book", 11);
        assertThat(b.getLastOverdueNotifiedAt()).isNull();

        when(borrowingRepository.findOverdueBooksNeedingReminder(any(LocalDateTime.class)))
            .thenReturn(List.of(b));

        scheduler.sendOverdueNotifications();

        assertThat(b.getLastOverdueNotifiedAt()).isNotNull();
    }

    @Test
    void shouldDoNothingWhenNoOverdueBooks() {
        when(borrowingRepository.findOverdueBooksNeedingReminder(any(LocalDateTime.class)))
            .thenReturn(List.of());

        scheduler.sendOverdueNotifications();

        verify(emailService, never()).sendOverdueWarning(any(), anyList());
        verify(borrowingRepository, never()).save(any());
    }

    // --- helpers ---

    private static User user(String username, UUID id) {
        return User.builder()
            .id(id).username(username).email(username + "@example.com")
            .name(username).surname("Last").role(UserRole.PATRON).status(UserStatus.ACTIVE)
            .build();
    }

    private static Borrowing overdue(User user, String bookName, int suffix) {
        Book book = Book.builder()
            .name(bookName).isbn("9780000000" + (100 + suffix)).author("A").publisher("P")
            .numberOfPages(1).quantity(1).availableQuantity(1).genre(BookGenre.OTHER).build();
        return Borrowing.builder()
            .book(book).user(user)
            .borrowDate(LocalDateTime.now().minusDays(30))
            .dueDate(LocalDateTime.now().minusDays(5))
            .returned(false)
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
