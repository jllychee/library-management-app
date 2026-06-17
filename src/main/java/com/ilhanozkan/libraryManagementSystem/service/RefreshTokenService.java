package com.ilhanozkan.libraryManagementSystem.service;

import com.ilhanozkan.libraryManagementSystem.model.entity.RefreshToken;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
  RefreshToken createRefreshToken(User user);
  Optional<RefreshToken> findByToken(String token);
  RefreshToken validateRefreshToken(String token);
  void revokeRefreshToken(String token);
  void revokeAllByUser(User user);
}
