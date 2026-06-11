package com.ilhanozkan.libraryManagementSystem.service;

import com.ilhanozkan.libraryManagementSystem.model.dto.response.FineResponseDTO;

import java.util.List;
import java.util.UUID;

public interface FineService {
  List<FineResponseDTO> getFines();
  List<FineResponseDTO> getFinesByUserId(UUID userId);
  int calculateOverdueFines();
}
