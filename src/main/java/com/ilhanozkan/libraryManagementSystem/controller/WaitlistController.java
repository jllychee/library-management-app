package com.ilhanozkan.libraryManagementSystem.controller;

import com.ilhanozkan.libraryManagementSystem.common.exception.book.BookNotFoundException;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.BookWaitlistRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.response.BookWaitlistResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.entity.UserPrincipal;
import com.ilhanozkan.libraryManagementSystem.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for patrons to manage book waitlists. When a waitlisted book transitions from
 * unavailable (0) to available (>0), the BookAvailabilityNotificationListener emails each
 * waitlisted patron once.
 *
 * All endpoints require an authenticated user. Served under the app's /api/v1 context path.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/waitlists")
@Tag(name = "Waitlist", description = "Patron book waitlist (availability notifications)")
@Slf4j
public class WaitlistController {

  private final WaitlistService waitlistService;

  @Operation(summary = "Join a book's waitlist", description = "Adds the authenticated patron to the waitlist for the given book")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully joined waitlist"),
      @ApiResponse(responseCode = "404", description = "Book not found"),
      @ApiResponse(responseCode = "409", description = "Already on waitlist for this book")
  })
  @PostMapping
  public ResponseEntity<?> joinWaitlist(@Valid @RequestBody BookWaitlistRequestDTO request) {
    User user = currentUser();
    log.info("Join waitlist: bookId={} for user={}", request.getBookId(), user.getId());
    try {
      BookWaitlistResponseDTO created = waitlistService.joinWaitlist(user, request.getBookId());
      return ResponseEntity.ok(created);
    } catch (BookNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    } catch (org.springframework.web.server.ResponseStatusException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    } catch (RuntimeException e) {
      log.error("Error joining waitlist: {}", e.getMessage());
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @Operation(summary = "List my waitlist entries", description = "Returns all of the authenticated patron's waitlist entries")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved waitlist")
  })
  @GetMapping("/mine")
  public ResponseEntity<List<BookWaitlistResponseDTO>> getMyWaitlist() {
    User user = currentUser();
    return ResponseEntity.ok(waitlistService.getMyWaitlist(user));
  }

  @Operation(summary = "Remove a waitlist entry", description = "Deletes one of the authenticated patron's own waitlist entries")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully removed"),
      @ApiResponse(responseCode = "404", description = "Entry not found (or not owned by caller)")
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> removeFromWaitlist(@PathVariable UUID id) {
    User user = currentUser();
    log.info("Remove waitlist id={} for user={}", id, user.getId());
    try {
      waitlistService.removeFromWaitlist(user, id);
      return ResponseEntity.ok().build();
    } catch (org.springframework.web.server.ResponseStatusException e) {
      return ResponseEntity.status(e.getStatusCode()).build();
    }
  }

  private User currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    return userPrincipal.getUser();
  }
}
