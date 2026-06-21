package com.ilhanozkan.libraryManagementSystem.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookWaitlistResponseDTO(
    UUID id,
    UUID bookId,
    String bookName,
    String isbn,
    String userName,
    LocalDateTime createdAt,
    LocalDateTime notifiedAt
) {
}
