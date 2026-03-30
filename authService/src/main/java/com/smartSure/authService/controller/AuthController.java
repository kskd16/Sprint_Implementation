package com.smartSure.authService.controller;

import com.smartSure.authService.dto.AuthResponseDto;
import com.smartSure.authService.dto.LoginRequestDto;
import com.smartSure.authService.dto.RegisterRequestDto;
import com.smartSure.authService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST Controller for SmartSure Insurance Management System.
 *
 * Provides endpoints for user registration and login.
 * All endpoints under /api/auth/** are publicly accessible (no JWT required).
 *
 * @author SmartSure Development Team
 * @version 2.1
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication API", description = "User registration and login")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user (CUSTOMER or ADMIN).
     * Public endpoint — no JWT required.
     *
     * @param request registration payload
     * @return 201 Created with success message, or 400 if email already exists
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Register request for email: {}", request.getEmail());
        try {
            String result = authService.register(request);
            log.info("Registration successful for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            log.warn("Registration failed for email: {} — {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Registration failed", e.getMessage()));
        }
    }

    /**
     * Authenticates a user and returns a signed JWT token.
     * Public endpoint — no JWT required.
     *
     * @param request login credentials
     * @return 200 OK with token, email, and role — or 401 if credentials are invalid
     */
    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Login request for email: {}", request.getEmail());
        try {
            AuthResponseDto response = authService.login(request);
            log.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Login failed for email: {} — {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed", "Invalid credentials"));
        }
    }

    /**
     * Health check — confirms the service is running.
     */
    @GetMapping("/health")
    @Operation(summary = "Service health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AuthService is running");
    }

    /**
     * Consistent error response structure.
     */
    public static class ErrorResponse {
        private final String error;
        private final String message;
        private final long timestamp;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getError()   { return error; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}
