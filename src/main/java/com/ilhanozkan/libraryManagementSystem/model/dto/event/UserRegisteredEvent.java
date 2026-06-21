package com.ilhanozkan.libraryManagementSystem.model.dto.event;

import java.util.UUID;

/**
 * Published by AuthServiceImpl.register() after the new user row is committed.
 * Consumed asynchronously by RegistrationNotificationListener.
 */
public record UserRegisteredEvent(
    UUID userId,
    String username,
    String email,
    String name,
    String surname
) {
}
