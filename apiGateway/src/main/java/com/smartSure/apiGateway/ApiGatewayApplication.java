package com.smartSure.apiGateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * SmartSure API Gateway Application
 * 
 * This is the main entry point for the SmartSure Insurance Management System's
 * API Gateway. The gateway serves as the single point of entry for all client
 * requests and provides the following capabilities:
 * 
 * Key Features:
 * - Centralized routing to all microservices
 * - Load balancing across service instances
 * - Rate limiting and throttling
 * - CORS handling for web clients
 * - Request/response logging and monitoring
 * - Circuit breaker patterns for fault tolerance
 * - Distributed tracing integration
 * 
 * Architecture Benefits:
 * - Single point of entry for all API calls
 * - Centralized cross-cutting concerns (auth, logging, monitoring)
 * - Service discovery integration with Eureka
 * - Reactive programming model for high throughput
 * - Redis-based caching and rate limiting
 * 
 * Service Routes:
 * - /api/auth/** → AuthService (port 8081)
 * - /api/policies/** → PolicyService (port 8082)
 * - /api/claims/** → ClaimService (port 8083)
 * - /api/admin/** → AdminService (port 8084)
 * - /api/payments/** → PaymentService (port 8085)
 * 
 * @author SmartSure Development Team
 * @version 2.0
 * @since 2024-03-25
 */
@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    /**
     * Main method to start the API Gateway application.
     * 
     * The application will:
     * 1. Initialize Spring Boot context
     * 2. Register with Eureka service registry
     * 3. Configure routing rules from properties
     * 4. Start the reactive web server on port 8080
     * 5. Begin accepting and routing requests
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        log.info("Starting SmartSure API Gateway...");
        log.info("Gateway will serve as the single entry point for all microservices");
        log.info("Configured routes: Auth, Policy, Claim, Admin, Payment services");
        
        try {
            SpringApplication.run(ApiGatewayApplication.class, args);
            log.info("SmartSure API Gateway started successfully on port 8080");
            log.info("Gateway is ready to route requests to microservices");
        } catch (Exception e) {
            log.error("Failed to start SmartSure API Gateway", e);
            System.exit(1);
        }
    }
}