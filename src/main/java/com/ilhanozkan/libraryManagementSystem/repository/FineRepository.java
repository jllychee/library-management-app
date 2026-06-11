package com.ilhanozkan.libraryManagementSystem.repository;

import com.ilhanozkan.libraryManagementSystem.model.entity.Borrowing;
import com.ilhanozkan.libraryManagementSystem.model.entity.Fine;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.enums.FineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FineRepository extends JpaRepository<Fine, UUID> {
  List<Fine> findByUserOrderByCreatedAtDesc(User user);
  Optional<Fine> findByBorrowing(Borrowing borrowing);
  Optional<Fine> findByBorrowingAndStatus(Borrowing borrowing, FineStatus status);
}
