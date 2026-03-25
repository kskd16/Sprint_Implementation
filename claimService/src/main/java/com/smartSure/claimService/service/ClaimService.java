package com.smartSure.claimService.service;

import com.smartSure.claimService.client.PolicyClient;
import com.smartSure.claimService.client.UserClient;
import com.smartSure.claimService.dto.ClaimRequest;
import com.smartSure.claimService.dto.ClaimResponse;
import com.smartSure.claimService.dto.PolicyDTO;
import com.smartSure.claimService.dto.UserResponseDto;
import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.FileData;
import com.smartSure.claimService.entity.Status;
import com.smartSure.claimService.exception.ClaimDeletionNotAllowedException;
import com.smartSure.claimService.exception.ClaimNotFoundException;
import com.smartSure.claimService.exception.DocumentNotUploadedException;
import com.smartSure.claimService.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyClient    policyClient;
    private final UserClient      userClient;
    private final EmailService    emailService;

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    public ClaimResponse createClaim(ClaimRequest request) {
        PolicyDTO policy = policyClient.getPolicyById(request.getPolicyId());
        Claim claim = new Claim();
        claim.setPolicyId(request.getPolicyId());
        claim.setAmount(policy.getAmount());
        return toResponse(claimRepository.save(claim));
    }

    public ClaimResponse getClaimById(Long claimId) {
        return toResponse(findOrThrow(claimId));
    }

    public List<ClaimResponse> getAllClaims() {
        return claimRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ClaimResponse> getAllUnderReviewClaims() {
        return claimRepository.findByStatus(Status.UNDER_REVIEW)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PolicyDTO getPolicyForClaim(Long claimId) {
        Claim claim = findOrThrow(claimId);
        return policyClient.getPolicyById(claim.getPolicyId());
    }

    public void deleteClaim(Long claimId) {
        Claim claim = findOrThrow(claimId);
        if (claim.getStatus() != Status.DRAFT) {
            throw new ClaimDeletionNotAllowedException(claimId);
        }
        claimRepository.deleteById(claimId);
    }

    // ─────────────────────────────────────────────
    // SUBMIT — Customer explicitly submits after uploading all 3 docs
    // ─────────────────────────────────────────────

    /**
     * Customer manually submits the claim after uploading all 3 documents.
     * Validates all 3 docs are present — throws DocumentNotUploadedException if any is missing.
     * On success: DRAFT → SUBMITTED → UNDER_REVIEW in a single call.
     */
    public ClaimResponse submitClaim(Long claimId) {
        Claim claim = findOrThrow(claimId);

        if (claim.getStatus() != Status.DRAFT) {
            throw new IllegalStateException(
                "Claim " + claimId + " cannot be submitted because it is not in DRAFT status.");
        }

        if (claim.getClaimForm() == null) {
            throw new DocumentNotUploadedException("Claim form", claimId);
        }
        if (claim.getAadhaarCard() == null) {
            throw new DocumentNotUploadedException("Aadhaar card", claimId);
        }
        if (claim.getEvidences() == null) {
            throw new DocumentNotUploadedException("Evidence", claimId);
        }

        // All docs present — DRAFT → SUBMITTED → UNDER_REVIEW
        claim.setStatus(claim.getStatus().moveTo(Status.SUBMITTED));
        claim.setStatus(claim.getStatus().moveTo(Status.UNDER_REVIEW));

        return toResponse(claimRepository.save(claim));
    }

    // ─────────────────────────────────────────────
    // STATUS TRANSITION — Admin only
    // ─────────────────────────────────────────────

    public ClaimResponse moveToStatus(Long claimId, Status nextStatus) {
        Claim claim = findOrThrow(claimId);
        claim.setStatus(claim.getStatus().moveTo(nextStatus));
        Claim saved = claimRepository.save(claim);
        if (nextStatus == Status.APPROVED || nextStatus == Status.REJECTED) {
            sendDecisionEmail(saved, nextStatus);
        }
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // DOCUMENT UPLOAD — no status change on upload
    // ─────────────────────────────────────────────

    public ClaimResponse uploadClaimForm(Long claimId, MultipartFile file) throws IOException {
        Claim claim = findOrThrow(claimId);
        claim.setClaimForm(toFileData(file));
        return toResponse(claimRepository.save(claim));
    }

    public ClaimResponse uploadAadhaarCard(Long claimId, MultipartFile file) throws IOException {
        Claim claim = findOrThrow(claimId);
        claim.setAadhaarCard(toFileData(file));
        return toResponse(claimRepository.save(claim));
    }

    public ClaimResponse uploadEvidence(Long claimId, MultipartFile file) throws IOException {
        Claim claim = findOrThrow(claimId);
        claim.setEvidences(toFileData(file));
        return toResponse(claimRepository.save(claim));
    }

    // ─────────────────────────────────────────────
    // DOCUMENT DOWNLOAD
    // ─────────────────────────────────────────────

    public FileData downloadClaimForm(Long claimId) {
        Claim claim = findOrThrow(claimId);
        if (claim.getClaimForm() == null) throw new DocumentNotUploadedException("Claim form", claimId);
        return claim.getClaimForm();
    }

    public FileData downloadAadhaarCard(Long claimId) {
        Claim claim = findOrThrow(claimId);
        if (claim.getAadhaarCard() == null) throw new DocumentNotUploadedException("Aadhaar card", claimId);
        return claim.getAadhaarCard();
    }

    public FileData downloadEvidence(Long claimId) {
        Claim claim = findOrThrow(claimId);
        if (claim.getEvidences() == null) throw new DocumentNotUploadedException("Evidence", claimId);
        return claim.getEvidences();
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private Claim findOrThrow(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));
    }

    private FileData toFileData(MultipartFile file) throws IOException {
        return new FileData(file.getOriginalFilename(), file.getContentType(), file.getBytes());
    }

    private void sendDecisionEmail(Claim claim, Status decision) {
        try {
            PolicyDTO policy = policyClient.getPolicyById(claim.getPolicyId());
            UserResponseDto user = userClient.getUserById(policy.getUserId());
            emailService.sendClaimDecisionEmail(
                user.getEmail(), user.getFirstName(), claim.getId(), decision.name());
        } catch (Exception e) {
            System.err.println("Failed to send claim decision email for claim "
                    + claim.getId() + ": " + e.getMessage());
        }
    }

    private ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getPolicyId(),
                claim.getAmount(),
                claim.getStatus(),
                claim.getTimeOfCreation(),
                claim.getClaimForm()   != null,
                claim.getAadhaarCard() != null,
                claim.getEvidences()   != null
        );
    }
}
