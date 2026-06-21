package com.ilhanozkan.libraryManagementSystem.service;

import com.ilhanozkan.libraryManagementSystem.model.dto.response.BookWaitlistResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;

import java.util.List;
import java.util.UUID;

public interface WaitlistService {

  BookWaitlistResponseDTO joinWaitlist(User user, UUID bookId);

  List<BookWaitlistResponseDTO> getMyWaitlist(User user);

  void removeFromWaitlist(User user, UUID waitlistId);
}
