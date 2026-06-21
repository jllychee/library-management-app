package com.ilhanozkan.libraryManagementSystem.service;

import com.ilhanozkan.libraryManagementSystem.model.dto.request.auth.LoginRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.auth.RefreshTokenRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.auth.RegisterRequestDTO;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;

public interface AuthService {
  public String register(RegisterRequestDTO registerRequestDTO) throws BadRequestException;
  public ResponseEntity<?> login(LoginRequestDTO loginRequestDTO);
  public ResponseEntity<?> refreshToken(RefreshTokenRequestDTO refreshTokenRequestDTO);
  public ResponseEntity<?> logout(RefreshTokenRequestDTO refreshTokenRequestDTO);
}
