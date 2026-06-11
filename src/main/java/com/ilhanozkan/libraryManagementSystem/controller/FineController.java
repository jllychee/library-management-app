package com.ilhanozkan.libraryManagementSystem.controller;

import com.ilhanozkan.libraryManagementSystem.model.dto.response.FineResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.entity.UserPrincipal;
import com.ilhanozkan.libraryManagementSystem.service.impl.FineServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Fine Operations")
@RestController
@RequestMapping("/fines")
@Slf4j
public class FineController {
  private final FineServiceImpl fineService;

  public FineController(FineServiceImpl fineService) {
    this.fineService = fineService;
    log.info("FineController initialized");
  }

  @Operation(summary = "Get all fines", description = "Retrieves all fines for librarians")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved fines"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  @GetMapping
  @PreAuthorize("hasRole('LIBRARIAN')")
  public List<FineResponseDTO> getFines() {
    log.info("Request to get all fines");
    return fineService.getFines();
  }

  @Operation(summary = "Get my fines", description = "Retrieves fines for the authenticated user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved fines")
  })
  @GetMapping("/my")
  public List<FineResponseDTO> getMyFines() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    User user = userPrincipal.getUser();

    log.info("Request to get fines for authenticated user ID: {}", user.getId());
    return fineService.getFinesByUserId(user.getId());
  }
}
