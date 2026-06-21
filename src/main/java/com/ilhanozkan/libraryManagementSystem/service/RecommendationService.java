package com.ilhanozkan.libraryManagementSystem.service;

import com.ilhanozkan.libraryManagementSystem.model.entity.Book;

import java.util.List;
import java.util.UUID;

public interface RecommendationService {
  List<Book> recommendBooks(UUID userId);
}