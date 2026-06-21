package com.ilhanozkan.libraryManagementSystem.service.impl;

import com.ilhanozkan.libraryManagementSystem.model.dto.request.auth.LoginRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.auth.RefreshTokenRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.request.auth.RegisterRequestDTO;
import com.ilhanozkan.libraryManagementSystem.model.dto.event.UserRegisteredEvent;
import com.ilhanozkan.libraryManagementSystem.model.dto.response.auth.LoginResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.RefreshToken;
import com.ilhanozkan.libraryManagementSystem.model.entity.User;
import com.ilhanozkan.libraryManagementSystem.model.enums.UserRole;
import com.ilhanozkan.libraryManagementSystem.repository.UserRepository;
import com.ilhanozkan.libraryManagementSystem.security.JwtService;
import com.ilhanozkan.libraryManagementSystem.service.RefreshTokenService;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDTO validRegisterRequest;
    private RegisterRequestDTO invalidPasswordRequest;
    private RegisterRequestDTO invalidEmailRequest;
    private LoginRequestDTO loginRequestDTO;
    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Setup valid registration request
        validRegisterRequest = new RegisterRequestDTO(
                "Test",
                "test.user@example.com",
                "Password123",  // Valid password (8+ characters)
                "Test",
                "User"
        );
        
        // Setup invalid password request
        invalidPasswordRequest = new RegisterRequestDTO(
                "Test",
                "test.user@example.com",
                "short",  // Too short password
                "Test",
                "User"
        );
        
        // Setup invalid email request
        invalidEmailRequest = new RegisterRequestDTO(
                "Test",
                "invalid-email",  // Invalid email format
                "Password123",
                "Test",
                "User"
        );

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .surname("User")
                .username("Test")
                .email("test.user@example.com")
                .password("encodedPassword")
                .build();

        // Setup for login
        loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setUsername("testuser");
        loginRequestDTO.setPassword("Password123");

        // Mock UserDetails with PATRON role
        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("encodedPassword")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATRON")))
                .build();
        
        // Setup common lenient stubs for all tests
        lenient().when(userRepository.existsByUsername(anyString())).thenReturn(false);
        lenient().when(userRepository.existsByEmail(anyString())).thenReturn(false);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        lenient().when(userRepository.save(any(User.class))).thenReturn(user);
    }

    @Test
    void shouldRegisterUserSuccessfully() throws BadRequestException {
        // Arrange - specific stubs for this test
        lenient().when(userRepository.existsByUsername("Test")).thenReturn(false);
        lenient().when(userRepository.existsByEmail("test.user@example.com")).thenReturn(false);
        lenient().when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");

        // Act
        String result = authService.register(validRegisterRequest);

        // Assert
        assertThat(result).contains("Test");
        assertThat(result).contains("registered successfully");
        
        verify(userRepository).existsByUsername("Test");
        verify(userRepository).existsByEmail("test.user@example.com");
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void shouldThrowExceptionWhenUsernameExists() {
        // Arrange - override default stub for this specific test
        lenient().when(userRepository.existsByUsername("Test")).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.register(validRegisterRequest));
        
        verify(userRepository).existsByUsername("Test");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange - override default stubs for this specific test
        lenient().when(userRepository.existsByUsername("Test")).thenReturn(false);
        lenient().when(userRepository.existsByEmail("test.user@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.register(validRegisterRequest));
        
        verify(userRepository).existsByUsername("Test");
        verify(userRepository).existsByEmail("test.user@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsTooShort() {
        // No need to override default stubs as they're already set up correctly for this test

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.register(invalidPasswordRequest));
        
        verify(userRepository).existsByUsername("Test");
        verify(userRepository).existsByEmail("test.user@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailFormatIsInvalid() {
        // No need to override default stubs as they're already set up correctly for this test

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.register(invalidEmailRequest));
        
        verify(userRepository).existsByUsername("Test");
        verify(userRepository).existsByEmail("invalid-email");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLoginSuccessfully() {
        // Arrange
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(loginRequestDTO.getUsername(), loginRequestDTO.getPassword());
        
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(jwtService.generateToken("testuser")).willReturn("jwt-token");
        given(userRepository.findByUsername("testuser")).willReturn(user);
        given(refreshTokenService.createRefreshToken(user)).willReturn(RefreshToken.builder()
                .token("refresh-token")
                .user(user)
                .build());

        // Act
        ResponseEntity<?> response = authService.login(loginRequestDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(LoginResponseDTO.class);
        
        LoginResponseDTO responseDTO = (LoginResponseDTO) response.getBody();
        assertThat(responseDTO.getToken()).isEqualTo("jwt-token");
        assertThat(responseDTO.getAccessToken()).isEqualTo("jwt-token");
        assertThat(responseDTO.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(responseDTO.getUsername()).isEqualTo("testuser");
        assertThat(responseDTO.getRole()).isEqualTo(UserRole.PATRON);
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken("testuser");
        verify(refreshTokenService).createRefreshToken(user);
    }

    @Test
    void shouldRefreshAccessTokenSuccessfully() {
        // Arrange
        RefreshTokenRequestDTO requestDTO = RefreshTokenRequestDTO.builder()
                .refreshToken("refresh-token")
                .build();
        user.setUsername("testuser");
        user.setRole(UserRole.PATRON);

        given(refreshTokenService.validateRefreshToken("refresh-token")).willReturn(RefreshToken.builder()
                .token("refresh-token")
                .user(user)
                .build());
        given(jwtService.generateToken("testuser")).willReturn("new-jwt-token");

        // Act
        ResponseEntity<?> response = authService.refreshToken(requestDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(LoginResponseDTO.class);

        LoginResponseDTO responseDTO = (LoginResponseDTO) response.getBody();
        assertThat(responseDTO.getToken()).isEqualTo("new-jwt-token");
        assertThat(responseDTO.getAccessToken()).isEqualTo("new-jwt-token");
        assertThat(responseDTO.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(responseDTO.getUsername()).isEqualTo("testuser");
        assertThat(responseDTO.getRole()).isEqualTo(UserRole.PATRON);

        verify(refreshTokenService).validateRefreshToken("refresh-token");
        verify(jwtService).generateToken("testuser");
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() {
        // Arrange
        RefreshTokenRequestDTO requestDTO = RefreshTokenRequestDTO.builder()
                .refreshToken("refresh-token")
                .build();

        // Act
        ResponseEntity<?> response = authService.logout(requestDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(refreshTokenService).revokeRefreshToken("refresh-token");
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginFails() {
        // Arrange
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .willThrow(new RuntimeException("Authentication failed"));

        // Act
        ResponseEntity<?> response = authService.login(loginRequestDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid username or password");
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(any());
    }
}
