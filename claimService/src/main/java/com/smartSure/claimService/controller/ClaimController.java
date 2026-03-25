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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    // POST /api/claims — creates a DRAFT claim
    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@RequestBody ClaimRequest request) {
        return ResponseEntity.ok(claimService.createClaim(request));
    }

    // GET /api/claims/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    // GET /api/claims — admin views all claims
    @GetMapping
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    // GET /api/claims/under-review — admin review queue
    @GetMapping("/under-review")
    public ResponseEntity<List<ClaimResponse>> getAllUnderReviewClaims() {
        return ResponseEntity.ok(claimService.getAllUnderReviewClaims());
    }

    // GET /api/claims/{id}/policy
    @GetMapping("/{id}/policy")
    public ResponseEntity<PolicyDTO> getPolicyForClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getPolicyForClaim(id));
    }

    // DELETE /api/claims/{id} — only DRAFT claims can be deleted
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        claimService.deleteClaim(id);
        return ResponseEntity.noContent().build();
    }

    // PUT /api/claims/{id}/submit
    // Customer uploads all 3 docs first, then calls this to submit.
    // Validates all docs present, then DRAFT → SUBMITTED → UNDER_REVIEW in one call.
    @PutMapping("/{id}/submit")
    public ResponseEntity<ClaimResponse> submitClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.submitClaim(id));
    }

    // PUT /api/claims/{id}/status?next=APPROVED — admin only
    @PutMapping("/{id}/status")
    public ResponseEntity<ClaimResponse> moveToStatus(
            @PathVariable Long id,
            @RequestParam Status next) {
        return ResponseEntity.ok(claimService.moveToStatus(id, next));
    }

    // POST /api/claims/{id}/upload/claim-form
    @PostMapping(value = "/{id}/upload/claim-form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClaimResponse> uploadClaimForm(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(claimService.uploadClaimForm(id, file));
    }

    // POST /api/claims/{id}/upload/aadhaar
    @PostMapping(value = "/{id}/upload/aadhaar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClaimResponse> uploadAadhaarCard(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(claimService.uploadAadhaarCard(id, file));
    }

    // POST /api/claims/{id}/upload/evidence
    @PostMapping(value = "/{id}/upload/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClaimResponse> uploadEvidence(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(claimService.uploadEvidence(id, file));
    }

    // GET /api/claims/{id}/download/claim-form
    @GetMapping("/{id}/download/claim-form")
    public ResponseEntity<byte[]> downloadClaimForm(@PathVariable Long id) {
        return buildFileResponse(claimService.downloadClaimForm(id));
    }

    // GET /api/claims/{id}/download/aadhaar
    @GetMapping("/{id}/download/aadhaar")
    public ResponseEntity<byte[]> downloadAadhaarCard(@PathVariable Long id) {
        return buildFileResponse(claimService.downloadAadhaarCard(id));
    }

    // GET /api/claims/{id}/download/evidence
    @GetMapping("/{id}/download/evidence")
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
