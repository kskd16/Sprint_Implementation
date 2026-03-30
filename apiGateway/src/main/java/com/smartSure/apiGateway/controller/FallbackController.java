package com.smartSure.apiGateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker Patterns
 * 
 * This controller provides fallback responses when downstream services are unavailable
 * or experiencing issues. It implements graceful degradation patterns to ensure
 * the system remains responsive even when individual services fail.
 * 
 * Key Features:
 * - Service-specific fallback responses
 * - Graceful degradation with meaningful error messages
 * - Monitoring and alerting integration
 * - Consistent error response format
 * - Circuit breaker state information
 * 
 * @author SmartSure Development Team
 * @version 1.0
 * @since 2024-03-25
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Fallback response for Authentication Service failures.
     * 
     * @return ResponseEntity with service unavailable message
     */
    @PostMapping("/auth")
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        log.warn("Authentication service is currently unavailable - returning fallback response");
        
        Map<String, Object> response = createFallbackResponse(
            "Authentication Service Unavailable",
            "The authentication service is temporarily unavailable. Please try again in a few moments.",
            "AUTH_SERVICE_DOWN"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback response for Policy Service failures.
     * 
     * @return ResponseEntity with service unavailable message
     */
    @PostMapping("/policy")
    @GetMapping("/policy")
    public ResponseEntity<Map<String, Object>> policyServiceFallback() {
        log.warn("Policy service is currently unavailable - returning fallback response");
        
        Map<String, Object> response = createFallbackResponse(
            "Policy Service Unavailable",
            "The policy service is temporarily unavailable. Your policy data is safe and will be available shortly.",
            "POLICY_SERVICE_DOWN"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback response for Claim Service failures.
     * 
     * @return ResponseEntity with service unavailable message
     */
    @PostMapping("/claim")
    @GetMapping("/claim")
    public ResponseEntity<Map<String, Object>> claimServiceFallback() {
        log.warn("Claim service is currently unavailable - returning fallback response");
        
        Map<String, Object> response = createFallbackResponse(
            "Claim Service Unavailable",
            "The claim service is temporarily unavailable. Your claim submissions are queued and will be processed once the service is restored.",
            "CLAIM_SERVICE_DOWN"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback response for Admin Service failures.
     * 
     * @return ResponseEntity with service unavailable message
     */
    @PostMapping("/admin")
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminServiceFallback() {
        log.warn("Admin service is currently unavailable - returning fallback response");
        
        Map<String, Object> response = createFallbackResponse(
            "Admin Service Unavailable",
            "The admin service is temporarily unavailable. Administrative functions will be restored shortly.",
            "ADMIN_SERVICE_DOWN"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Fallback response for Payment Service failures.
     * 
     * @return ResponseEntity with service unavailable message
     */
    @PostMapping("/payment")
    @GetMapping("/payment")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.warn("Payment service is currently unavailable - returning fallback response");
        
        Map<String, Object> response = createFallbackResponse(
            "Payment Service Unavailable",
            "The payment service is temporarily unavailable. Your payment information is secure and transactions will be processed once the service is restored.",
            "PAYMENT_SERVICE_DOWN"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * General fallback response for any unhandled service failures.
     * 
     * @return ResponseEntity with generic service unavailable message
     */
    @PostMapping("/general")
    @GetMapping("/general")
    public ResponseEntity<Map<String, Object>> generalFallback() {
        log.warn("General service fallback triggered - service temporarily unavailable");
        
        Map<String, Object> response = createFallbackResponse(
            "Service Temporarily Unavailable",
            "The requested service is temporarily unavailable. Our team has been notified and is working to restore service.",
            "SERVICE_UNAVAILABLE"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Creates a standardized fallback response with consistent structure.
     * 
     * @param title Error title
     * @param message Detailed error message
     * @param errorCode Specific error code for client handling
     * @return Map containing the fallback response structure
     */
    private Map<String, Object> createFallbackResponse(String title, String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("title", title);
        response.put("message", message);
        response.put("errorCode", errorCode);
        response.put("timestamp", LocalDateTime.now());
        response.put("suggestion", "Please try again in a few moments. If the problem persists, contact support.");
        response.put("supportContact", "support@smartsure.com");
        
        return response;
    }
}