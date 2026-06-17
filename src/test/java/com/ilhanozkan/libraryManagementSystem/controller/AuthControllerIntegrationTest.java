package com.ilhanozkan.libraryManagementSystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.auth.LoginRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.auth.RefreshTokenRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.auth.RegisterRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserRole;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserStatus;
import com.ilhanozkan.libraryManagementSystem.repository.RefreshTokenRepository;
import com.ilhanozkan.libraryManagementSystem.repository.UserRepository;
import com.ilhanozkan.libraryManagementSystem.repository.BorrowingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BorrowingRepository borrowingRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        // Clean up repository
        refreshTokenRepository.deleteAll();
        borrowingRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user for login tests
        User user = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .password(passwordEncoder.encode("password"))
                .name("Existing")
                .surname("User")
                .role(UserRole.PATRON)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
    }

    @Test
    public void shouldRegisterNewUser() throws Exception {
        // Given
        RegisterRequestDTO registerRequestDTO = new RegisterRequestDTO(
                "newuser", 
                "new@example.com",
                "password123", // Using longer password to meet validation
                "New",
                "User"
        );

        // When
        ResultActions response = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequestDTO)));

        // Then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("newuser")))
                .andDo(print());
    }

    @Test
    public void shouldNotRegisterUserWithExistingUsername() throws Exception {
        // Given
        RegisterRequestDTO registerRequestDTO = new RegisterRequestDTO(
                "existinguser", // Already exists
                "another@example.com",
                "password123", // Using longer password to meet validation
                "Another",
                "User"
        );

        // When
        ResultActions response = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequestDTO)));

        // Then
        response.andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    public void shouldNotRegisterUserWithExistingEmail() throws Exception {
        // Given
        RegisterRequestDTO registerRequestDTO = new RegisterRequestDTO(
                "anotheruser",
                "existing@example.com", // Already exists
                "password123", // Using longer password to meet validation
                "Another",
                "User"
        );

        // When
        ResultActions response = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequestDTO)));

        // Then
        response.andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    public void shouldLoginWithValidCredentials() throws Exception {
        // Given
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setUsername("existinguser");
        loginRequestDTO.setPassword("password");

        // When
        ResultActions response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDTO)));

        // Then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.username", is("existinguser")))
                .andDo(print());
    }

    @Test
    public void shouldRefreshAccessTokenWithValidRefreshToken() throws Exception {
        // Given
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setUsername("existinguser");
        loginRequestDTO.setPassword("password");

        String loginResponse = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDTO)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();
        RefreshTokenRequestDTO refreshTokenRequestDTO = RefreshTokenRequestDTO.builder()
                .refreshToken(refreshToken)
                .build();

        // When
        ResultActions response = mockMvc.perform(post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequestDTO)));

        // Then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", is(refreshToken)))
                .andExpect(jsonPath("$.username", is("existinguser")))
                .andDo(print());
    }

    @Test
    public void shouldRejectInvalidRefreshToken() throws Exception {
        // Given
        RefreshTokenRequestDTO refreshTokenRequestDTO = RefreshTokenRequestDTO.builder()
                .refreshToken("invalid-refresh-token")
                .build();

        // When
        ResultActions response = mockMvc.perform(post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequestDTO)));

        // Then
        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andDo(print());
    }

    @Test
    public void shouldRevokeRefreshTokenOnLogout() throws Exception {
        // Given
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setUsername("existinguser");
        loginRequestDTO.setPassword("password");

        String loginResponse = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDTO)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();
        RefreshTokenRequestDTO refreshTokenRequestDTO = RefreshTokenRequestDTO.builder()
                .refreshToken(refreshToken)
                .build();

        // When
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andDo(print());

        // Then
        mockMvc.perform(post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequestDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andDo(print());
    }

    @Test
    public void shouldNotLoginWithInvalidUsername() throws Exception {
        // Given
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setUsername("nonexistentuser");
        loginRequestDTO.setPassword("password");

        // When
        ResultActions response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDTO)));

        // Then
        response.andExpect(status().isUnauthorized())
                .andDo(print());
    }

    @Test
    public void shouldNotLoginWithInvalidPassword() throws Exception {
        // Given
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setUsername("existinguser");
        loginRequestDTO.setPassword("wrongpassword");

        // When
        ResultActions response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDTO)));

        // Then
        response.andExpect(status().isUnauthorized())
                .andDo(print());
    }
} 
