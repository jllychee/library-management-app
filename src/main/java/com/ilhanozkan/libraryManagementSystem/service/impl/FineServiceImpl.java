package com.ilhanozkan.libraryManagementSystem.service.impl;

import com.ilhanozkan.libraryManagementSystem.common.exception.user.UserNotFoundException;
import com.ilhanozkan.libraryManagementSystem.model.dto.response.FineResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.Fine;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.enums.FineStatus;
import com.ilhanozkan.libraryManagementSystem.model.mapper.FineResponseDTOMapper;
import com.ilhanozkan.libraryManagementSystem.repository.BorrowingRepository;
import com.ilhanozkan.libraryManagementSystem.repository.FineRepository;
import com.ilhanozkan.libraryManagementSystem.repository.UserRepository;
import com.ilhanozkan.libraryManagementSystem.service.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FineServiceImpl implements FineService {
  private final FineRepository fineRepository;
  private final BorrowingRepository borrowingRepository;
  private final UserRepository userRepository;
  private final FineResponseDTOMapper mapper = FineResponseDTOMapper.INSTANCE;

  @Value("${library.fines.daily-rate:1.00}")
  private BigDecimal dailyRate;

  @Transactional(readOnly = true)
  public List<FineResponseDTO> getFines() {
    log.debug("Fetching all fines");
    return mapper.toFineResponseDTOList(fineRepository.findAll());
  }

  @Transactional(readOnly = true)
  public List<FineResponseDTO> getFinesByUserId(UUID userId) {
    log.debug("Fetching fines for user ID: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("User not found with ID: {}", userId);
          return new UserNotFoundException(userId);
        });

    return mapper.toFineResponseDTOList(fineRepository.findByUserOrderByCreatedAtDesc(user));
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kuala_Lumpur")
  @Transactional
  public int calculateOverdueFines() {
    return calculateOverdueFines(LocalDateTime.now());
  }

  @Transactional
  int calculateOverdueFines(LocalDateTime now) {
    List<Borrowing> overdueBorrowings = borrowingRepository.findOverdueBooks();
    int processedCount = 0;

    for (Borrowing borrowing : overdueBorrowings) {
      long overdueDays = ChronoUnit.DAYS.between(borrowing.getDueDate().toLocalDate(), now.toLocalDate());
      if (overdueDays <= 0) {
        continue;
      }

      BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(overdueDays));
      Fine fine = fineRepository.findByBorrowingAndStatus(borrowing, FineStatus.UNPAID)
          .orElseGet(() -> Fine.builder()
              .borrowing(borrowing)
              .book(borrowing.getBook())
              .user(borrowing.getUser())
              .status(FineStatus.UNPAID)
              .createdAt(now)
              .build());

      fine.setAmount(amount);
      fine.setOverdueDays(overdueDays);
      fine.setCalculatedUntil(now);
      fine.setUpdatedAt(now);
      fineRepository.save(fine);
      processedCount++;
    }

    log.info("Processed {} overdue fines", processedCount);
    return processedCount;
  }
}
