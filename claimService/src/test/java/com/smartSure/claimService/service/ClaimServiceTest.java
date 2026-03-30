package com.smartSure.claimService.service;

import com.smartSure.claimService.client.PolicyClient;
import com.smartSure.claimService.client.UserClient;
import com.smartSure.claimService.dto.ClaimRequest;
import com.smartSure.claimService.dto.ClaimResponse;
import com.smartSure.claimService.dto.PolicyDTO;
import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.FileData;
import com.smartSure.claimService.entity.Status;
import com.smartSure.claimService.exception.ClaimDeletionNotAllowedException;
import com.smartSure.claimService.exception.ClaimNotFoundException;
import com.smartSure.claimService.exception.DocumentNotUploadedException;
import com.smartSure.claimService.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClaimService.
 * Tests claim creation, document upload, submission, status transitions, and deletion.
 */
@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private PolicyClient policyClient;
    @Mock private UserClient userClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private ClaimService claimService;

    private Claim mockClaim;
    private PolicyDTO mockPolicy;

    @BeforeEach
    void setUp() {
        mockClaim = new Claim();
        mockClaim.setStatus(Status.DRAFT);
        mockClaim.setPolicyId(1L);
        mockClaim.setAmount(new BigDecimal("50000.00"));

        mockPolicy = new PolicyDTO();
        mockPolicy.setPolicyID(1L);
        mockPolicy.setAmount(new BigDecimal("50000.00"));
        mockPolicy.setUserId(1L);
    }

    @Test
    @DisplayName("Create claim - creates DRAFT claim with amount from policy")
    void createClaim_createsDraftClaim() {
        ClaimRequest request = new ClaimRequest();
        request.setPolicyId(1L);

        when(policyClient.getPolicyById(1L)).thenReturn(mockPolicy);
        when(claimRepository.save(any(Claim.class))).thenReturn(mockClaim);

        ClaimResponse response = claimService.createClaim(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Status.DRAFT);
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    @DisplayName("Get claim by ID - returns claim when found")
    void getClaimById_returnsClaimWhenFound() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        ClaimResponse response = claimService.getClaimById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getPolicyId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get claim by ID - throws when not found")
    void getClaimById_throwsWhenNotFound() {
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.getClaimById(99L))
                .isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    @DisplayName("Get all claims - returns all claims")
    void getAllClaims_returnsAll() {
        when(claimRepository.findAll()).thenReturn(List.of(mockClaim));

        List<ClaimResponse> result = claimService.getAllClaims();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Delete claim - succeeds for DRAFT status")
    void deleteClaim_succeedsForDraft() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        assertThatCode(() -> claimService.deleteClaim(1L)).doesNotThrowAnyException();
        verify(claimRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete claim - throws for non-DRAFT status")
    void deleteClaim_throwsForNonDraft() {
        mockClaim.setStatus(Status.SUBMITTED);
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        assertThatThrownBy(() -> claimService.deleteClaim(1L))
                .isInstanceOf(ClaimDeletionNotAllowedException.class);

        verify(claimRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Submit claim - throws when claim form not uploaded")
    void submitClaim_throwsWhenClaimFormMissing() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        assertThatThrownBy(() -> claimService.submitClaim(1L))
                .isInstanceOf(DocumentNotUploadedException.class);
    }

    @Test
    @DisplayName("Submit claim - throws when not in DRAFT status")
    void submitClaim_throwsWhenNotDraft() {
        mockClaim.setStatus(Status.SUBMITTED);
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        assertThatThrownBy(() -> claimService.submitClaim(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be submitted");
    }

    @Test
    @DisplayName("Submit claim - transitions to UNDER_REVIEW when all docs uploaded")
    void submitClaim_transitionsToUnderReviewWhenAllDocsUploaded() {
        mockClaim.setClaimForm(new FileData("form.pdf", "application/pdf", new byte[]{1}));
        mockClaim.setAadhaarCard(new FileData("aadhaar.pdf", "application/pdf", new byte[]{1}));
        mockClaim.setEvidences(new FileData("evidence.pdf", "application/pdf", new byte[]{1}));

        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));
        when(claimRepository.save(any(Claim.class))).thenReturn(mockClaim);

        ClaimResponse response = claimService.submitClaim(1L);

        assertThat(response.getStatus()).isEqualTo(Status.UNDER_REVIEW);
    }

    @Test
    @DisplayName("Move to status - APPROVED publishes RabbitMQ event")
    void moveToStatus_approvedPublishesEvent() {
        mockClaim.setStatus(Status.UNDER_REVIEW);
        mockClaim.setClaimForm(new FileData("f.pdf", "application/pdf", new byte[]{1}));

        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));
        when(claimRepository.save(any(Claim.class))).thenReturn(mockClaim);
        when(policyClient.getPolicyById(anyLong())).thenReturn(mockPolicy);
        when(userClient.getUserById(anyLong())).thenReturn(
                new com.smartSure.claimService.dto.UserResponseDto());

        claimService.moveToStatus(1L, Status.APPROVED);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Get all under review claims - returns only UNDER_REVIEW")
    void getAllUnderReviewClaims_returnsOnlyUnderReview() {
        mockClaim.setStatus(Status.UNDER_REVIEW);
        when(claimRepository.findByStatus(Status.UNDER_REVIEW)).thenReturn(List.of(mockClaim));

        List<ClaimResponse> result = claimService.getAllUnderReviewClaims();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Status.UNDER_REVIEW);
    }
}
