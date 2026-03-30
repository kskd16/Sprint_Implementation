package com.smartSure.authService.service;

import com.smartSure.authService.dto.AuthResponseDto;
import com.smartSure.authService.dto.LoginRequestDto;
import com.smartSure.authService.dto.RegisterRequestDto;
import com.smartSure.authService.entity.Role;
import com.smartSure.authService.entity.User;
import com.smartSure.authService.repository.UserRepository;
import com.smartSure.authService.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests registration, login, duplicate email, and invalid credentials scenarios.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private AuthService authService;

    private User mockUser;
    private RegisterRequestDto registerRequest;
    private LoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setEmail("rahul@example.com");
        mockUser.setPassword("encodedPassword");
        mockUser.setRole(Role.CUSTOMER);
        mockUser.setFirstName("Rahul");

        registerRequest = new RegisterRequestDto();
        registerRequest.setFirstName("Rahul");
        registerRequest.setLastName("Sharma");
        registerRequest.setEmail("rahul@example.com");
        registerRequest.setPassword("Test@1234");
        registerRequest.setRole("CUSTOMER");

        loginRequest = new LoginRequestDto();
        loginRequest.setEmail("rahul@example.com");
        loginRequest.setPassword("Test@1234");
    }

    @Test
    @DisplayName("Register - success when email is new")
    void register_success() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(modelMapper.map(registerRequest, User.class)).thenReturn(mockUser);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        String result = authService.register(registerRequest);

        assertThat(result).isEqualTo("User registered successfully");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register - throws when email already exists")
    void register_duplicateEmail_throws() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login - success with valid credentials")
    void login_success() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), mockUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(mockUser.getUserId(), mockUser.getRole().name())).thenReturn("jwt.token.here");

        AuthResponseDto response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getEmail()).isEqualTo("rahul@example.com");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Login - throws when user not found")
    void login_userNotFound_throws() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("Login - throws when password is wrong")
    void login_wrongPassword_throws() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), mockUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");

        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Register - role is set correctly from request")
    void register_roleSetCorrectly() {
        registerRequest.setRole("ADMIN");
        User adminUser = new User();
        adminUser.setUserId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Role.ADMIN);

        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(modelMapper.map(registerRequest, User.class)).thenReturn(adminUser);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        String result = authService.register(registerRequest);

        assertThat(result).isEqualTo("User registered successfully");
    }
}
