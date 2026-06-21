package com.ilhanozkan.libraryManagementSystem.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class BookWaitlistRequestDTO {
  @NotNull(message = "bookId must not be null")
  private UUID bookId;
}
