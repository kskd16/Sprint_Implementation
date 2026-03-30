package com.smartSure.adminService.service;

import com.smartSure.adminService.dto.ClaimDTO;
import com.smartSure.adminService.dto.PolicyDTO;
import com.smartSure.adminService.dto.PolicyStatusUpdateRequest;
import com.smartSure.adminService.dto.UserDTO;
import com.smartSure.adminService.entity.AuditLog;
import com.smartSure.adminService.feign.ClaimFeignClient;
import com.smartSure.adminService.feign.PolicyFeignClient;
import com.smartSure.adminService.feign.UserFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminService.
 * Tests claim approval/rejection, policy management, user management, and audit logging.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private ClaimFeignClient claimFeignClient;
    @Mock private PolicyFeignClient policyFeignClient;
    @Mock private UserFeignClient userFeignClient;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private AdminService adminService;

    private ClaimDTO mockClaim;
    private PolicyDTO mockPolicy;
    private UserDTO mockUser;

    @BeforeEach
    void setUp() {
        mockClaim = new ClaimDTO();
        mockClaim.setId(1L);
        mockClaim.setStatus("UNDER_REVIEW");
        mockClaim.setAmount(new BigDecimal("50000.00"));

        mockPolicy = new PolicyDTO();
        mockPolicy.setId(1L);
        mockPolicy.setStatus("ACTIVE");

        mockUser = new UserDTO();
        mockUser.setUserId(1L);
        mockUser.setEmail("rahul@example.com");
    }

    @Test
    @DisplayName("Get all claims - returns all claims from claimService")
    void getAllClaims_returnsAll() {
        when(claimFeignClient.getAllClaims()).thenReturn(List.of(mockClaim));

        List<ClaimDTO> result = adminService.getAllClaims();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get under review claims - returns only UNDER_REVIEW claims")
    void getUnderReviewClaims_returnsUnderReview() {
        when(claimFeignClient.getUnderReviewClaims()).thenReturn(List.of(mockClaim));

        List<ClaimDTO> result = adminService.getUnderReviewClaims();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("UNDER_REVIEW");
    }

    @Test
    @DisplayName("Approve claim - updates status and logs action")
    void approveClaim_updatesStatusAndLogs() {
        ClaimDTO approvedClaim = new ClaimDTO();
        approvedClaim.setId(1L);
        approvedClaim.setStatus("APPROVED");

        when(claimFeignClient.updateClaimStatus(1L, "APPROVED")).thenReturn(approvedClaim);
        when(auditLogService.log(anyLong(), anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new AuditLog());

        ClaimDTO result = adminService.approveClaim(1L, 1L, "All docs verified");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verify(auditLogService).log(eq(1L), eq("APPROVE_CLAIM"), eq("Claim"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Reject claim - updates status and logs action")
    void rejectClaim_updatesStatusAndLogs() {
        ClaimDTO rejectedClaim = new ClaimDTO();
        rejectedClaim.setId(1L);
        rejectedClaim.setStatus("REJECTED");

        when(claimFeignClient.updateClaimStatus(1L, "REJECTED")).thenReturn(rejectedClaim);
        when(auditLogService.log(anyLong(), anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new AuditLog());

        ClaimDTO result = adminService.rejectClaim(1L, 1L, "Insufficient evidence");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        verify(auditLogService).log(eq(1L), eq("REJECT_CLAIM"), eq("Claim"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Get all policies - returns all policies from policyService")
    void getAllPolicies_returnsAll() {
        when(policyFeignClient.getAllPolicies()).thenReturn(List.of(mockPolicy));

        List<PolicyDTO> result = adminService.getAllPolicies();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Cancel policy - updates status and logs action")
    void cancelPolicy_updatesStatusAndLogs() {
        PolicyDTO cancelledPolicy = new PolicyDTO();
        cancelledPolicy.setId(1L);
        cancelledPolicy.setStatus("CANCELLED");

        when(policyFeignClient.updatePolicyStatus(eq(1L), any(PolicyStatusUpdateRequest.class)))
                .thenReturn(cancelledPolicy);
        when(auditLogService.log(anyLong(), anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new AuditLog());

        PolicyDTO result = adminService.cancelPolicy(1L, 1L, "Fraud detected");

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(auditLogService).log(eq(1L), eq("CANCEL_POLICY"), eq("Policy"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Get all users - returns all users from authService")
    void getAllUsers_returnsAll() {
        when(userFeignClient.getAllUsers()).thenReturn(List.of(mockUser));

        List<UserDTO> result = adminService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("rahul@example.com");
    }

    @Test
    @DisplayName("Get user by ID - returns correct user")
    void getUserById_returnsUser() {
        when(userFeignClient.getUserById(1L)).thenReturn(mockUser);

        UserDTO result = adminService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Mark under review - skips transition if already UNDER_REVIEW")
    void markUnderReview_skipsIfAlreadyUnderReview() {
        mockClaim.setStatus("UNDER_REVIEW");
        when(claimFeignClient.getClaimById(1L)).thenReturn(mockClaim);
        when(auditLogService.log(anyLong(), anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new AuditLog());

        ClaimDTO result = adminService.markUnderReview(1L, 1L);

        assertThat(result.getStatus()).isEqualTo("UNDER_REVIEW");
        verify(claimFeignClient, never()).updateClaimStatus(any(), any());
    }
}
