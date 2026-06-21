package com.ilhanozkan.libraryManagementSystem.service;

import com.ilhanozkan.libraryManagementSystem.model.dto.response.BookResponseDTO;

import java.util.List;
import java.util.UUID;

public interface RecommendationService {
  List<BookResponseDTO> recommendBooks(UUID userId);
}
