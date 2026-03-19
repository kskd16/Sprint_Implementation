package com.smartSure.claimService.repository;

import com.smartSure.claimService.entity.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {

    // Get all documents attached to a specific claim
    List<ClaimDocument> findByClaimId(Long claimId);

    // Count documents for a claim — to enforce upload limits if needed
    long countByClaimId(Long claimId);
}
