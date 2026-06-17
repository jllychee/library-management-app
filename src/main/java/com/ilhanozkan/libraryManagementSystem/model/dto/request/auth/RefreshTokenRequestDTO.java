package com.ilhanozkan.libraryManagementSystem.model.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRequestDTO {
  @NotBlank(message = "Refresh token is required")
  private String refreshToken;
}
