package com.smartSure.claimService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> map = new HashMap<>();
        map.put("Error", "Forbidden — insufficient role");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(map);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        Map<String, String> map = new HashMap<>();
        map.put("Error", "Unauthorized — valid JWT required");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(map);
    }

    @ExceptionHandler(ClaimNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleClaimNotFound(ClaimNotFoundException ex) {
        Map<String, String> map = new HashMap<>();
        map.put("Error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTransition(InvalidStatusTransitionException ex) {
        Map<String, String> map = new HashMap<>();
        map.put("Error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(map);
    }

    @ExceptionHandler(ClaimDeletionNotAllowedException.class)
    public ResponseEntity<Map<String, String>> handleDeletionNotAllowed(ClaimDeletionNotAllowedException ex) {
        Map<String, String> map = new HashMap<>();
        map.put("Error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(map);
    }

    @ExceptionHandler(DocumentNotUploadedException.class)
    public ResponseEntity<Map<String, String>> handleDocumentNotUploaded(DocumentNotUploadedException ex) {
        Map<String, String> map = new HashMap<>();
        map.put("Error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        Map<String, String> map = new HashMap<>();
        map.put("Error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
    }
}
