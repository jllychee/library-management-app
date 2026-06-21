package com.ilhanozkan.libraryManagementSystem.repository;

import com.ilhanozkan.libraryManagementSystem.model.entity.RefreshToken;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByToken(String token);

  List<RefreshToken> findByUser(User user);

  @Modifying
  @Query("update RefreshToken r set r.revoked = true where r.user = :user and r.revoked = false")
  void revokeAllByUser(User user);
}
