package com.ilhanozkan.libraryManagementSystem.model.dto.response;

import com.ilhanozkan.libraryManagementSystem.model.enums.FineStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FineResponseDTO(
    UUID id,
    UUID borrowingId,
    BookResponseDTO book,
    UserResponseDTO user,
    BigDecimal amount,
    FineStatus status,
    Long overdueDays,
    LocalDateTime calculatedUntil,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
