package com.ilhanozkan.libraryManagementSystem.model.dto.event;

import java.util.UUID;

/**
 * Published by BookServiceImpl when a book's available quantity transitions from
 * 0 (or non-existent) to strictly greater than 0. Distinct from the reactive
 * {@code BookAvailabilityEvent} (a record streamed over SSE); this one is a Spring
 * ApplicationEvent consumed by BookAvailabilityNotificationListener to email
 * waitlisted patrons.
 */
public record BookBecameAvailableEvent(
    UUID bookId,
    String bookName,
    String isbn,
    int availableQuantity
) {
}
