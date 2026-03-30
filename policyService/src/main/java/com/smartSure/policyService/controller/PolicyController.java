package com.smartSure.policyService.controller;

import com.smartSure.policyService.dto.calculation.PremiumCalculationRequest;
import com.smartSure.policyService.dto.calculation.PremiumCalculationResponse;
import com.smartSure.policyService.dto.policy.*;
import com.smartSure.policyService.dto.premium.PremiumPaymentRequest;
import com.smartSure.policyService.dto.premium.PremiumResponse;
import com.smartSure.policyService.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Policy REST Controller for SmartSure Insurance Management System.
 *
 * User identity is extracted from the JWT principal set by JwtAuthFilter.
 * No X-User-Id or X-User-Role headers are required from the client.
 *
 * @author SmartSure Development Team
 * @version 2.1
 */
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Policies", description = "Policy purchase, management, and premium payment")
public class PolicyController {

    private final PolicyService policyService;

    /**
     * Purchase a new insurance policy.
     * Customer only — userId extracted from JWT principal.
     */
    @PostMapping("/purchase")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Purchase a new insurance policy")
    public ResponseEntity<PolicyResponse> purchasePolicy(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PolicyPurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(policyService.purchasePolicy(Long.parseLong(userId), request));
    }

    /**
     * Get all policies belonging to the authenticated customer.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get all my policies")
    public ResponseEntity<List<PolicyResponse>> getMyPolicies(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(policyService.getCustomerPolicies(Long.parseLong(userId)));
    }

    /**
     * Get a single policy by ID.
     * Admin can view any policy; customer can only view their own.
     * Role is determined from the JWT — no header needed.
     */
    @GetMapping("/{policyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get policy details by ID")
    public ResponseEntity<PolicyResponse> getPolicyById(
            @PathVariable Long policyId,
            @AuthenticationPrincipal String userId) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return ResponseEntity.ok(
                policyService.getPolicyById(policyId, Long.parseLong(userId), isAdmin));
    }

    /**
     * Cancel a policy owned by the authenticated customer.
     */
    @PutMapping("/{policyId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cancel my policy")
    public ResponseEntity<PolicyResponse> cancelPolicy(
            @PathVariable Long policyId,
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(
                policyService.cancelPolicy(policyId, Long.parseLong(userId), reason));
    }

    /**
     * Renew an existing policy.
     */
    @PostMapping("/renew")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Renew an existing policy")
    public ResponseEntity<PolicyResponse> renewPolicy(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PolicyRenewalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(policyService.renewPolicy(Long.parseLong(userId), request));
    }

    /**
     * Pay a premium installment directly via policyService (without Razorpay).
     */
    @PostMapping("/premiums/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Pay a premium installment")
    public ResponseEntity<PremiumResponse> payPremium(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PremiumPaymentRequest request) {
        return ResponseEntity.ok(policyService.payPremium(Long.parseLong(userId), request));
    }

    /**
     * Get the full premium schedule for a policy.
     */
    @GetMapping("/{policyId}/premiums")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get premium schedule for a policy")
    public ResponseEntity<List<PremiumResponse>> getPremiums(@PathVariable Long policyId) {
        return ResponseEntity.ok(policyService.getPremiumsByPolicy(policyId));
    }

    /**
     * Calculate estimated premium before purchasing — public, no auth required.
     */
    @PostMapping("/calculate-premium")
    @Operation(summary = "Calculate premium before purchase (no auth required)")
    public ResponseEntity<PremiumCalculationResponse> calculatePremium(
            @Valid @RequestBody PremiumCalculationRequest request) {
        return ResponseEntity.ok(policyService.calculatePremium(request));
    }

    /**
     * Admin — get all policies across all customers.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all policies (Admin only)")
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    /**
     * Admin — update policy status (ACTIVE, CANCELLED, etc.).
     */
    @PutMapping("/admin/{policyId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update policy status (Admin only)")
    public ResponseEntity<PolicyResponse> adminUpdateStatus(
            @PathVariable Long policyId,
            @Valid @RequestBody PolicyStatusUpdateRequest request) {
        return ResponseEntity.ok(policyService.adminUpdatePolicyStatus(policyId, request));
    }

    /**
     * Admin — get policy statistics summary.
     */
    @GetMapping("/admin/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get policy statistics summary (Admin only)")
    public ResponseEntity<PolicySummaryResponse> getPolicySummary() {
        return ResponseEntity.ok(policyService.getPolicySummary());
    }
}
