package com.smartSure.policyService.service;

import com.smartSure.policyService.dto.policy.PolicyPurchaseRequest;
import com.smartSure.policyService.dto.policy.PolicyResponse;
import com.smartSure.policyService.dto.policy.PolicyStatusUpdateRequest;
import com.smartSure.policyService.entity.Policy;
import com.smartSure.policyService.entity.PolicyType;
import com.smartSure.policyService.entity.Premium;
import com.smartSure.policyService.mapper.PolicyMapper;
import com.smartSure.policyService.repository.PolicyRepository;
import com.smartSure.policyService.repository.PolicyTypeRepository;
import com.smartSure.policyService.repository.PremiumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PolicyService.
 * Tests policy purchase, retrieval, cancellation, and admin operations.
 */
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock private PolicyRepository policyRepository;
    @Mock private PolicyTypeRepository policyTypeRepository;
    @Mock private PremiumRepository premiumRepository;
    @Mock private PremiumCalculator premiumCalculator;
    @Mock private PolicyMapper policyMapper;

    @InjectMocks private PolicyService policyService;

    private PolicyType mockPolicyType;
    private Policy mockPolicy;
    private PolicyResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockPolicyType = new PolicyType();
        mockPolicyType.setId(1L);
        mockPolicyType.setName("SmartHealth Plus");
        mockPolicyType.setBasePremium(new BigDecimal("5000.00"));
        mockPolicyType.setMaxCoverageAmount(new BigDecimal("1000000.00"));
        mockPolicyType.setTermMonths(12);
        mockPolicyType.setStatus(PolicyType.PolicyTypeStatus.ACTIVE);

        mockPolicy = Policy.builder()
                .id(1L)
                .policyNumber("POL-20250101-ABCDE")
                .customerId(1L)
                .policyType(mockPolicyType)
                .coverageAmount(new BigDecimal("500000.00"))
                .premiumAmount(new BigDecimal("5000.00"))
                .paymentFrequency(Policy.PaymentFrequency.ANNUAL)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(12))
                .status(Policy.PolicyStatus.ACTIVE)
                .build();

        mockResponse = PolicyResponse.builder()
                .id(1L)
                .policyNumber("POL-20250101-ABCDE")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Get customer policies - returns list for valid customer")
    void getCustomerPolicies_returnsListForCustomer() {
        when(policyRepository.findByCustomerId(1L)).thenReturn(List.of(mockPolicy));
        when(policyMapper.toResponse(mockPolicy)).thenReturn(mockResponse);

        List<PolicyResponse> result = policyService.getCustomerPolicies(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPolicyNumber()).isEqualTo("POL-20250101-ABCDE");
    }

    @Test
    @DisplayName("Get customer policies - returns empty list when no policies")
    void getCustomerPolicies_returnsEmptyList() {
        when(policyRepository.findByCustomerId(99L)).thenReturn(List.of());

        List<PolicyResponse> result = policyService.getCustomerPolicies(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Get policy by ID - admin can access any policy")
    void getPolicyById_adminCanAccessAnyPolicy() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));
        when(premiumRepository.findByPolicyId(1L)).thenReturn(List.of());
        when(policyMapper.toResponseWithPremiums(eq(mockPolicy), anyList())).thenReturn(mockResponse);

        PolicyResponse result = policyService.getPolicyById(1L, 99L, true);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Get policy by ID - customer cannot access another customer's policy")
    void getPolicyById_customerCannotAccessOtherPolicy() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));

        // mockPolicy.customerId = 1L, but requesting userId = 2L, isAdmin = false
        assertThatThrownBy(() -> policyService.getPolicyById(1L, 2L, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("Cancel policy - success for policy owner")
    void cancelPolicy_successForOwner() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));
        when(premiumRepository.findByPolicyIdAndStatus(1L, Premium.PremiumStatus.PENDING)).thenReturn(List.of());
        when(policyRepository.save(any(Policy.class))).thenReturn(mockPolicy);
        when(policyMapper.toResponse(mockPolicy)).thenReturn(mockResponse);

        PolicyResponse result = policyService.cancelPolicy(1L, 1L, "No longer needed");

        assertThat(result).isNotNull();
        verify(policyRepository).save(argThat(p -> p.getStatus() == Policy.PolicyStatus.CANCELLED));
    }

    @Test
    @DisplayName("Cancel policy - throws when customer does not own policy")
    void cancelPolicy_throwsForNonOwner() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));

        assertThatThrownBy(() -> policyService.cancelPolicy(1L, 2L, "reason"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    @DisplayName("Admin update policy status - updates correctly")
    void adminUpdatePolicyStatus_updatesStatus() {
        PolicyStatusUpdateRequest request = new PolicyStatusUpdateRequest(Policy.PolicyStatus.CANCELLED, "Fraud");
        when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));
        when(policyRepository.save(any(Policy.class))).thenReturn(mockPolicy);
        when(policyMapper.toResponse(mockPolicy)).thenReturn(mockResponse);

        PolicyResponse result = policyService.adminUpdatePolicyStatus(1L, request);

        assertThat(result).isNotNull();
        verify(policyRepository).save(argThat(p -> p.getStatus() == Policy.PolicyStatus.CANCELLED));
    }

    @Test
    @DisplayName("Get all policies - returns all for admin")
    void getAllPolicies_returnsAll() {
        when(policyRepository.findAll()).thenReturn(List.of(mockPolicy));
        when(policyMapper.toResponse(mockPolicy)).thenReturn(mockResponse);

        List<PolicyResponse> result = policyService.getAllPolicies();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Purchase policy - throws when policy type not found")
    void purchasePolicy_throwsWhenTypeNotFound() {
        PolicyPurchaseRequest request = new PolicyPurchaseRequest();
        request.setPolicyTypeId(99L);
        request.setCoverageAmount(new BigDecimal("500000"));
        request.setPaymentFrequency(Policy.PaymentFrequency.ANNUAL);
        request.setStartDate(LocalDate.now());

        when(policyTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.purchasePolicy(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Policy type not found");
    }

    @Test
    @DisplayName("Purchase policy - throws when policy type is inactive")
    void purchasePolicy_throwsWhenTypeInactive() {
        mockPolicyType.setStatus(PolicyType.PolicyTypeStatus.INACTIVE);
        PolicyPurchaseRequest request = new PolicyPurchaseRequest();
        request.setPolicyTypeId(1L);
        request.setCoverageAmount(new BigDecimal("500000"));
        request.setPaymentFrequency(Policy.PaymentFrequency.ANNUAL);
        request.setStartDate(LocalDate.now());

        when(policyTypeRepository.findById(1L)).thenReturn(Optional.of(mockPolicyType));

        assertThatThrownBy(() -> policyService.purchasePolicy(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inactive policy type");
    }
}
