package com.smartSure.claimService.controller;

import com.smartSure.claimService.dto.ClaimRequest;
import com.smartSure.claimService.dto.ClaimResponse;
import com.smartSure.claimService.dto.PolicyDTO;
import com.smartSure.claimService.entity.FileData;
import com.smartSure.claimService.entity.Status;
import com.smartSure.claimService.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Claim REST Controller for SmartSure Insurance Management System.
 *
 * Authorization rules:
 * - CUSTOMER: create, upload docs, submit, view own claims, download own docs, delete DRAFT
 * - ADMIN: view all claims, move status (APPROVED/REJECTED)
 * - Both: view single claim by ID
 *
 * User identity is extracted from JWT principal — no headers required.
 *
 * @author SmartSure Development Team
 * @version 2.1
 */
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    /**
     * Create a new DRAFT claim.
     * Customer only — userId extracted from JWT principal.
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> createClaim(
            @AuthenticationPrincipal String userId,
            @RequestBody ClaimRequest request) {
        return ResponseEntity.ok(claimService.createClaim(Long.parseLong(userId), request));
    }

    /**
     * Get a single claim by ID.
     * Both CUSTOMER and ADMIN can access.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    /**
     * Get all claims — admin view.
     * ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    /**
     * Get all claims currently under review — admin dashboard.
     * ADMIN only.
     */
    @GetMapping("/under-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllUnderReviewClaims() {
        return ResponseEntity.ok(claimService.getAllUnderReviewClaims());
    }

    /**
     * Get the linked policy details for a claim.
     * Both CUSTOMER and ADMIN can access.
     */
    @GetMapping("/{id}/policy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PolicyDTO> getPolicyForClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getPolicyForClaim(id));
    }

    /**
     * Delete a claim — only allowed when status is DRAFT.
     * CUSTOMER only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        claimService.deleteClaim(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Submit a claim after all 3 documents are uploaded.
     * Transitions: DRAFT → SUBMITTED → UNDER_REVIEW in one call.
     * CUSTOMER only.
     */
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> submitClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.submitClaim(id));
    }

    /**
     * Move claim to APPROVED or REJECTED.
     * ADMIN only — triggers RabbitMQ event → email sent to customer.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClaimResponse> moveToStatus(
            @PathVariable Long id,
            @RequestParam Status next) {
        return ResponseEntity.ok(claimService.moveToStatus(id, next));
    }

    /**
     * Upload claim form document.
     * CUSTOMER only — claim must be in DRAFT status.
     */
    @PostMapping(value = "/{id}/upload/claim-form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadClaimForm(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(claimService.uploadClaimForm(id, file));
    }

    /**
     * Upload Aadhaar card document.
     * CUSTOMER only — claim must be in DRAFT status.
     */
    @PostMapping(value = "/{id}/upload/aadhaar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadAadhaarCard(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(claimService.uploadAadhaarCard(id, file));
    }

    /**
     * Upload evidence document.
     * CUSTOMER only — claim must be in DRAFT status.
     */
    @PostMapping(value = "/{id}/upload/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadEvidence(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(claimService.uploadEvidence(id, file));
    }

    /**
     * Download claim form document.
     * Both CUSTOMER and ADMIN can access.
     */
    @GetMapping("/{id}/download/claim-form")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadClaimForm(@PathVariable Long id) {
        return buildFileResponse(claimService.downloadClaimForm(id));
    }

    /**
     * Download Aadhaar card document.
     * Both CUSTOMER and ADMIN can access.
     */
    @GetMapping("/{id}/download/aadhaar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadAadhaarCard(@PathVariable Long id) {
        return buildFileResponse(claimService.downloadAadhaarCard(id));
    }

    /**
     * Download evidence document.
     * Both CUSTOMER and ADMIN can access.
     */
    @GetMapping("/{id}/download/evidence")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id) {
        return buildFileResponse(claimService.downloadEvidence(id));
    }

    private ResponseEntity<byte[]> buildFileResponse(FileData file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .body(file.getData());
    }
}
