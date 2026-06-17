package com.ilhanozkan.libraryManagementSystem.model.dto.response.auth;

import com.ilhanozkan.libraryManagementSystem.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
  private String token;
  private String accessToken;
  private String refreshToken;
  private String username;
  private UserRole role;
}
