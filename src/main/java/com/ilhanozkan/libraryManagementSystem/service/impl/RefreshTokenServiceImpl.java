package com.ilhanozkan.libraryManagementSystem.service.impl;

import com.ilhanozkan.libraryManagementSystem.common.exception.auth.InvalidRefreshTokenException;
import com.ilhanozkan.libraryManagementSystem.model.entity.RefreshToken;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.repository.RefreshTokenRepository;
import com.ilhanozkan.libraryManagementSystem.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {
  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${jwt.refresh-expiration}")
  private long refreshTokenExpiration;

  @Override
  @Transactional
  public RefreshToken createRefreshToken(User user) {
    RefreshToken refreshToken = RefreshToken.builder()
        .token(UUID.randomUUID().toString())
        .user(user)
        .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
        .revoked(false)
        .build();

    RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
    log.debug("Refresh token created for user: {}", user.getUsername());
    return savedToken;
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
  }

  @Override
  @Transactional
  public RefreshToken validateRefreshToken(String token) {
    RefreshToken refreshToken = findByToken(token)
        .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

    if (refreshToken.isRevoked()) {
      throw new InvalidRefreshTokenException("Refresh token has been revoked");
    }

    if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
      refreshToken.setRevoked(true);
      refreshTokenRepository.save(refreshToken);
      throw new InvalidRefreshTokenException("Refresh token has expired");
    }

    return refreshToken;
  }

  @Override
  @Transactional
  public void revokeRefreshToken(String token) {
    RefreshToken refreshToken = findByToken(token)
        .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

    refreshToken.setRevoked(true);
    refreshTokenRepository.save(refreshToken);
    log.debug("Refresh token revoked for user: {}", refreshToken.getUser().getUsername());
  }

  @Override
  @Transactional
  public void revokeAllByUser(User user) {
    refreshTokenRepository.revokeAllByUser(user);
  }
}
