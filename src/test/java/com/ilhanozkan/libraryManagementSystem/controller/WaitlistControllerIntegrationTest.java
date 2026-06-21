package com.ilhanozkan.libraryManagementSystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilhanozkan.libraryManagementSystem.config.MailConfig.CapturingJavaMailSender;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.BookWaitlistRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.Book;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.enums.BookGenre;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserRole;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserStatus;
import com.ilhanozkan.libraryManagementSystem.repository.BookRepository;
import com.ilhanozkan.libraryManagementSystem.repository.BookWaitlistRepository;
import com.ilhanozkan.libraryManagementSystem.repository.BorrowingRepository;
import com.ilhanozkan.libraryManagementSystem.repository.UserRepository;
import com.ilhanozkan.libraryManagementSystem.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-context integration test for {@link WaitlistController}. Mirrors the existing
 * controller-test flavor: @SpringBootTest + MockMvc + @ActiveProfiles("test"), wiping
 * repos in @BeforeEach and minting real JWTs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WaitlistControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BorrowingRepository borrowingRepository;
    @Autowired private BookWaitlistRepository waitlistRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JavaMailSender mailSender;

    private User patron;
    private Book unavailableBook;
    private String patronToken;

    @BeforeEach
    void setUp() {
        // Wipe in FK-safe order (borrowings/waitlist reference books+users).
        // DataInitializer seeds users/books/borrowings at context startup.
        waitlistRepository.deleteAll();
        borrowingRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
        ((CapturingJavaMailSender) mailSender).clear();

        patron = userRepository.save(User.builder()
            .username("patron").email("patron@test.com").password("password")
            .name("Patron").surname("User").role(UserRole.PATRON).status(UserStatus.ACTIVE)
            .build());
        patronToken = jwtService.generateToken(patron.getUsername());

        // A book currently checked out (0 available) -> valid waitlist target.
        unavailableBook = bookRepository.save(Book.builder()
            .name("Dune").isbn("9780441172719").author("Herbert").publisher("Ace")
            .numberOfPages(400).quantity(1).availableQuantity(0).genre(BookGenre.SCIENCE_FICTION)
            .build());
    }

    @Test
    void shouldJoinWaitlist() throws Exception {
        BookWaitlistRequestDTO body = new BookWaitlistRequestDTO();
        body.setBookId(unavailableBook.getId());

        mockMvc.perform(post("/waitlists")
                .header("Authorization", "Bearer " + patronToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.bookId").value(unavailableBook.getId().toString()))
            .andExpect(jsonPath("$.notifiedAt").isEmpty())
            .andDo(print());

        assertThat(waitlistRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateWaitlistEntryWith409() throws Exception {
        joinWaitlist(unavailableBook.getId()); // first join succeeds

        BookWaitlistRequestDTO body = new BookWaitlistRequestDTO();
        body.setBookId(unavailableBook.getId());

        mockMvc.perform(post("/waitlists")
                .header("Authorization", "Bearer " + patronToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict())
            .andDo(print());

        assertThat(waitlistRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldListMyWaitlist() throws Exception {
        UUID entryId = joinWaitlist(unavailableBook.getId());

        mockMvc.perform(get("/waitlists/mine")
                .header("Authorization", "Bearer " + patronToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(entryId.toString()))
            .andExpect(jsonPath("$[0].bookName").value("Dune"))
            .andDo(print());
    }

    @Test
    void shouldDeleteMyEntry() throws Exception {
        UUID entryId = joinWaitlist(unavailableBook.getId());

        mockMvc.perform(delete("/waitlists/{id}", entryId)
                .header("Authorization", "Bearer " + patronToken))
            .andExpect(status().isOk())
            .andDo(print());

        assertThat(waitlistRepository.findById(entryId)).isEmpty();
    }

    @Test
    void joiningNonexistentBookReturns404() throws Exception {
        BookWaitlistRequestDTO body = new BookWaitlistRequestDTO();
        body.setBookId(UUID.randomUUID());

        mockMvc.perform(post("/waitlists")
                .header("Authorization", "Bearer " + patronToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound())
            .andDo(print());
    }

  // --- helpers ---

  /** Performs a join and returns the created entry id. */
  private UUID joinWaitlist(UUID bookId) throws Exception {
    BookWaitlistRequestDTO body = new BookWaitlistRequestDTO();
    body.setBookId(bookId);
    String json = mockMvc.perform(post("/waitlists")
            .header("Authorization", "Bearer " + patronToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andReturn().getResponse().getContentAsString();
    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
  }
}
