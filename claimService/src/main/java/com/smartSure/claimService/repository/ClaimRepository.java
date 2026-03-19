package com.smartSure.claimService.repository;

import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    // Get all claims submitted by a specific user
    List<Claim> findByUserId(Long userId);

    // Get all claims linked to a specific policy
    List<Claim> findByPolicyId(Long policyId);

    // Get all claims by status — for admin review queue
    List<Claim> findByStatus(ClaimStatus status);

    // Get claims by user and status — for user's filtered view
    List<Claim> findByUserIdAndStatus(Long userId, ClaimStatus status);

    // Count claims per status — for admin dashboard stats
    @Query("SELECT c.status, COUNT(c) FROM Claim c GROUP BY c.status")
    List<Object[]> countClaimsByStatus();

    // Check if a claim already exists for a policy in PENDING or UNDER_REVIEW state
    @Query("SELECT COUNT(c) > 0 FROM Claim c WHERE c.policyId = :policyId AND c.status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW')")
    boolean hasActiveClaim(@Param("policyId") Long policyId);
}
