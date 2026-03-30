package com.smartSure.authService.service;

import com.smartSure.authService.dto.AuthResponseDto;
import com.smartSure.authService.dto.LoginRequestDto;
import com.smartSure.authService.dto.RegisterRequestDto;
import com.smartSure.authService.entity.Role;
import com.smartSure.authService.entity.User;
import com.smartSure.authService.repository.UserRepository;
import com.smartSure.authService.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service for SmartSure Insurance Management System.
 *
 * Handles user registration and login. Redis caching is applied via
 * the CacheManager (declared in CacheConfig) — this service does NOT
 * hold a direct RedisTemplate dependency so it starts cleanly even
 * when Redis is unavailable.
 *
 * @author SmartSure Development Team
 * @version 2.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;

    /**
     * Registers a new user.
     * Validates email uniqueness, encrypts password, assigns role, and persists.
     *
     * @param request registration payload
     * @return success message
     * @throws RuntimeException if email already exists
     */
    public String register(RegisterRequestDto request) {
        log.info("Registration request for email: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Duplicate registration attempt for email: {}", request.getEmail());
            throw new RuntimeException("Email already registered");
        }

        User user = modelMapper.map(request, User.class);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.valueOf(request.getRole().toUpperCase()));

        User saved = userRepository.save(user);
        log.info("User registered — id={}, email={}", saved.getUserId(), saved.getEmail());

        return "User registered successfully";
    }

    /**
     * Authenticates a user and returns a signed JWT token.
     * JWT subject is the userId (Long as String) so all downstream
     * services can extract a numeric user ID from the token.
     *
     * @param request login credentials
     * @return AuthResponseDto containing token, email, and role
     * @throws RuntimeException if credentials are invalid
     */
    public AuthResponseDto login(LoginRequestDto request) {
        log.info("Login request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — email not found: {}", request.getEmail());
                    return new RuntimeException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed — wrong password for email: {}", request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        // Subject = userId so downstream services receive a Long ID, not an email
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole().name());

        log.info("Login successful — userId={}, role={}", user.getUserId(), user.getRole());
        return new AuthResponseDto(token, user.getEmail(), user.getRole().name());
    }
}
