package com.ilhanozkan.libraryManagementSystem.common.exception.auth;

public class InvalidRefreshTokenException extends RuntimeException {
  public InvalidRefreshTokenException(String message) {
    super(message);
  }
}
