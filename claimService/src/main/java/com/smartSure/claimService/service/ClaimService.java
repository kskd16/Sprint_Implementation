package com.smartSure.claimService.service;

import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.ClaimDocument;
import com.smartSure.claimService.entity.ClaimStatus;
import com.smartSure.claimService.exception.InvalidClaimStateException;
import com.smartSure.claimService.exception.ResourceNotFoundException;
import com.smartSure.claimService.repository.ClaimDocumentRepository;
import com.smartSure.claimService.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository claimDocumentRepository;

    // Create a new claim in DRAFT state
    public Claim createClaim(Claim claim) {
        claim.setStatus(ClaimStatus.DRAFT);
        return claimRepository.save(claim);
    }

    // Submit a claim — only allowed from DRAFT state
    public Claim submitClaim(Long claimId) {
        Claim claim = getClaimById(claimId);
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidClaimStateException("Only DRAFT claims can be submitted. Current status: " + claim.getStatus());
        }
        if (claimRepository.hasActiveClaim(claim.getPolicyId())) {
            throw new InvalidClaimStateException("An active claim already exists for this policy.");
        }
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setUpdatedAt(LocalDateTime.now());
        return claimRepository.save(claim);
    }

    // Get claim by ID
    public Claim getClaimById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
    }

    // Get all claims for a user
    public List<Claim> getClaimsByUser(Long userId) {
        return claimRepository.findByUserId(userId);
    }

    // Get all claims by status — for admin review queue
    public List<Claim> getClaimsByStatus(ClaimStatus status) {
        return claimRepository.findByStatus(status);
    }

    // Update claim status — used by admin to approve/reject
    public Claim updateClaimStatus(Long claimId, ClaimStatus newStatus, String remarks) {
        Claim claim = getClaimById(claimId);

        // Only SUBMITTED or UNDER_REVIEW claims can be approved/rejected
        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new InvalidClaimStateException("Claim is already finalized with status: " + claim.getStatus());
        }
        claim.setStatus(newStatus);
        claim.setUpdatedAt(LocalDateTime.now());
        return claimRepository.save(claim);
    }

    // Add a document to a claim — only allowed if claim is in DRAFT or SUBMITTED
    public ClaimDocument addDocument(Long claimId, String fileName, String fileUrl) {
        Claim claim = getClaimById(claimId);
        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new InvalidClaimStateException("Cannot add documents to a finalized claim.");
        }
        ClaimDocument doc = new ClaimDocument();
        doc.setClaim(claim);
        doc.setFileName(fileName);
        doc.setFileUrl(fileUrl);
        return claimDocumentRepository.save(doc);
    }

    // Get all documents for a claim
    public List<ClaimDocument> getDocumentsByClaimId(Long claimId) {
        return claimDocumentRepository.findByClaimId(claimId);
    }

    // Get all claims — for admin
    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }

    // Get claim status summary — for admin dashboard
    public List<Object[]> getClaimStatusSummary() {
        return claimRepository.countClaimsByStatus();
    }
}
