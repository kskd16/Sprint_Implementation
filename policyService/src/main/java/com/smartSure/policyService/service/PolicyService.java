package com.smartSure.policyService.service;

import com.smartSure.policyService.dto.calculation.PremiumCalculationRequest;
import com.smartSure.policyService.dto.calculation.PremiumCalculationResponse;
import com.smartSure.policyService.dto.policy.*;
import com.smartSure.policyService.dto.premium.PremiumPaymentRequest;
import com.smartSure.policyService.dto.premium.PremiumResponse;
import com.smartSure.policyService.entity.Policy;
import com.smartSure.policyService.entity.PolicyType;
import com.smartSure.policyService.entity.Premium;
import com.smartSure.policyService.mapper.PolicyMapper;
import com.smartSure.policyService.repository.PolicyRepository;
import com.smartSure.policyService.repository.PolicyTypeRepository;
import com.smartSure.policyService.repository.PremiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final PremiumRepository premiumRepository;
    private final PremiumCalculator premiumCalculator;
    private final PolicyMapper policyMapper;

    @Transactional
    public PolicyResponse purchasePolicy(Long customerId, PolicyPurchaseRequest request) {
        log.info("Purchasing policy for customer {}", customerId);

        PolicyType type = policyTypeRepository.findById(request.getPolicyTypeId())
                .orElseThrow(() -> new RuntimeException("Policy type not found"));

        if (type.getStatus() != PolicyType.PolicyTypeStatus.ACTIVE) {
            throw new RuntimeException("Inactive policy type");
        }

        BigDecimal premiumAmount = premiumCalculator.calculatePremium(
                type, request.getCoverageAmount(), request.getPaymentFrequency(), request.getCustomerAge()
        ).getCalculatedPremium();

        Policy policy = policyMapper.toEntity(request);
        policy.setCustomerId(customerId);
        policy.setPolicyType(type);
        policy.setPremiumAmount(premiumAmount);
        policy.setPolicyNumber(generatePolicyNumber());
        policy.setEndDate(request.getStartDate().plusMonths(type.getTermMonths()));
        policy.setStatus(
                request.getStartDate().isAfter(LocalDate.now())
                        ? Policy.PolicyStatus.CREATED
                        : Policy.PolicyStatus.ACTIVE
        );

        Policy saved = policyRepository.save(policy);
        generatePremiumSchedule(saved, type.getTermMonths());
        return buildDetailedResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> getCustomerPolicies(Long customerId) {
        return policyRepository.findByCustomerId(customerId)
                .stream().map(policyMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicyById(Long policyId, Long userId, boolean isAdmin) {
        Policy policy = getPolicy(policyId);
        if (!isAdmin && !policy.getCustomerId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        return buildDetailedResponse(policy);
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> getAllPolicies() {
        return policyRepository.findAll().stream().map(policyMapper::toResponse).toList();
    }

    @Transactional
    public PolicyResponse cancelPolicy(Long policyId, Long customerId, String reason) {
        Policy policy = getPolicy(policyId);
        if (!policy.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Unauthorized");
        }
        policy.setStatus(Policy.PolicyStatus.CANCELLED);
        policy.setCancellationReason(reason);
        premiumRepository.findByPolicyIdAndStatus(policyId, Premium.PremiumStatus.PENDING)
                .forEach(p -> p.setStatus(Premium.PremiumStatus.WAIVED));
        return policyMapper.toResponse(policyRepository.save(policy));
    }

    @Transactional
    public PolicyResponse renewPolicy(Long customerId, PolicyRenewalRequest request) {
        Policy oldPolicy = getPolicy(request.getPolicyId());
        if (!oldPolicy.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Unauthorized");
        }
        oldPolicy.setStatus(Policy.PolicyStatus.EXPIRED);

        PolicyType type = oldPolicy.getPolicyType();
        BigDecimal coverage = request.getNewCoverageAmount() != null
                ? request.getNewCoverageAmount() : oldPolicy.getCoverageAmount();
        Policy.PaymentFrequency freq = request.getPaymentFrequency() != null
                ? request.getPaymentFrequency() : oldPolicy.getPaymentFrequency();
        BigDecimal premium = premiumCalculator.calculatePremium(type, coverage, freq, null).getCalculatedPremium();

        Policy newPolicy = Policy.builder()
                .policyNumber(generatePolicyNumber())
                .customerId(customerId)
                .policyType(type)
                .coverageAmount(coverage)
                .premiumAmount(premium)
                .paymentFrequency(freq)
                .startDate(oldPolicy.getEndDate())
                .endDate(request.getNewEndDate())
                .status(Policy.PolicyStatus.ACTIVE)
                .build();

        Policy saved = policyRepository.save(newPolicy);
        generatePremiumSchedule(saved, type.getTermMonths());
        return buildDetailedResponse(saved);
    }

    @Transactional
    public PremiumResponse payPremium(Long customerId, PremiumPaymentRequest request) {
        Policy policy = getPolicy(request.getPolicyId());
        if (!policy.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Unauthorized");
        }
        Premium premium = premiumRepository.findByIdAndPolicyId(request.getPremiumId(), request.getPolicyId())
                .orElseThrow(() -> new RuntimeException("Premium not found"));

        premium.setStatus(Premium.PremiumStatus.PAID);
        premium.setPaidDate(LocalDate.now());
        premium.setPaymentMethod(request.getPaymentMethod());
        premium.setPaymentReference(
                request.getPaymentReference() != null
                        ? request.getPaymentReference()
                        : "TXN-" + UUID.randomUUID().toString().substring(0, 8)
        );
        return mapPremium(premiumRepository.save(premium));
    }

    @Transactional(readOnly = true)
    public List<PremiumResponse> getPremiumsByPolicy(Long policyId) {
        return premiumRepository.findByPolicyId(policyId).stream().map(this::mapPremium).toList();
    }

    @Transactional
    public PolicyResponse adminUpdatePolicyStatus(Long policyId, PolicyStatusUpdateRequest request) {
        Policy policy = getPolicy(policyId);
        policy.setStatus(request.getStatus());
        policy.setCancellationReason(request.getReason());
        return policyMapper.toResponse(policyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public PolicySummaryResponse getPolicySummary() {
        return PolicySummaryResponse.builder()
                .totalPolicies(policyRepository.count())
                .activePolicies(policyRepository.countByStatus(Policy.PolicyStatus.ACTIVE))
                .expiredPolicies(policyRepository.countByStatus(Policy.PolicyStatus.EXPIRED))
                .cancelledPolicies(policyRepository.countByStatus(Policy.PolicyStatus.CANCELLED))
                .totalPremiumCollected(premiumRepository.totalPremiumCollected(Premium.PremiumStatus.PAID))
                .totalCoverageProvided(policyRepository.sumActiveCoverages())
                .build();
    }

    @Transactional(readOnly = true)
    public PremiumCalculationResponse calculatePremium(PremiumCalculationRequest request) {
        PolicyType type = policyTypeRepository.findById(request.getPolicyTypeId())
                .orElseThrow(() -> new RuntimeException("Policy type not found"));
        return premiumCalculator.calculatePremium(
                type, request.getCoverageAmount(), request.getPaymentFrequency(), request.getCustomerAge()
        );
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expirePolicies() {
        policyRepository.findExpiredActivePolicies(Policy.PolicyStatus.ACTIVE, LocalDate.now())
                .forEach(p -> p.setStatus(Policy.PolicyStatus.EXPIRED));
    }

    private Policy getPolicy(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
    }

    private PolicyResponse buildDetailedResponse(Policy policy) {
        return policyMapper.toResponseWithPremiums(policy, getPremiumsByPolicy(policy.getId()));
    }

    private PremiumResponse mapPremium(Premium premium) {
        return PremiumResponse.builder()
                .id(premium.getId())
                .amount(premium.getAmount())
                .dueDate(premium.getDueDate())
                .paidDate(premium.getPaidDate())
                .status(premium.getStatus().name())
                .paymentReference(premium.getPaymentReference())
                .paymentMethod(premium.getPaymentMethod() != null ? premium.getPaymentMethod().name() : null)
                .build();
    }

    private String generatePolicyNumber() {
        return "POL-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    private void generatePremiumSchedule(Policy policy, int termMonths) {
        int interval = premiumCalculator.monthsBetweenInstallments(policy.getPaymentFrequency());
        int count = premiumCalculator.installmentCount(termMonths, policy.getPaymentFrequency());
        LocalDate dueDate = policy.getStartDate();
        List<Premium> premiums = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            premiums.add(Premium.builder()
                    .policy(policy)
                    .amount(policy.getPremiumAmount())
                    .dueDate(dueDate)
                    .status(Premium.PremiumStatus.PENDING)
                    .build());
            dueDate = dueDate.plusMonths(interval);
        }
        premiumRepository.saveAll(premiums);
    }
}
