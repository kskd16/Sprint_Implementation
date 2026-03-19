package com.smartSure.policyService.service;

import com.smartSure.policyService.dto.calculation.PremiumCalculationResponse;
import com.smartSure.policyService.entity.Policy;
import com.smartSure.policyService.entity.PolicyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PremiumCalculator {

    private static final BigDecimal BASE_COVERAGE_UNIT = new BigDecimal("100000");

    public PremiumCalculationResponse calculatePremium(
            PolicyType policyType,
            BigDecimal coverageAmount,
            Policy.PaymentFrequency frequency,
            Integer age
    ) {
        BigDecimal coverageFactor = calculateCoverageFactor(coverageAmount);
        BigDecimal ageFactor = calculateAgeFactor(age);
        BigDecimal annualPremium = calculateAnnual(policyType, coverageAmount, age);
        BigDecimal perInstallment = applyFrequency(annualPremium, frequency);

        return PremiumCalculationResponse.builder()
                .basePremium(policyType.getBasePremium())
                .annualPremium(annualPremium)
                .calculatedPremium(perInstallment)
                .paymentFrequency(frequency.name())
                .breakdown(buildBreakdown(policyType, coverageFactor, ageFactor, frequency))
                .build();
    }

    public BigDecimal calculateAnnual(PolicyType policyType, BigDecimal coverageAmount, Integer age) {
        return policyType.getBasePremium()
                .multiply(calculateCoverageFactor(coverageAmount))
                .multiply(calculateAgeFactor(age))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCoverageFactor(BigDecimal coverageAmount) {
        return coverageAmount.divide(BASE_COVERAGE_UNIT, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAgeFactor(Integer age) {
        if (age == null) return BigDecimal.ONE;
        if (age < 25) return new BigDecimal("0.85");
        if (age < 35) return BigDecimal.ONE;
        if (age < 45) return new BigDecimal("1.20");
        if (age < 55) return new BigDecimal("1.50");
        if (age < 65) return new BigDecimal("1.90");
        return new BigDecimal("2.50");
    }

    private BigDecimal applyFrequency(BigDecimal annualPremium, Policy.PaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> annualPremium.multiply(new BigDecimal("1.05")).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            case QUARTERLY -> annualPremium.multiply(new BigDecimal("1.03")).divide(new BigDecimal("4"), 2, RoundingMode.HALF_UP);
            case SEMI_ANNUAL -> annualPremium.multiply(new BigDecimal("1.01")).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            case ANNUAL -> annualPremium;
        };
    }

    public int installmentCount(int termMonths, Policy.PaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> termMonths;
            case QUARTERLY -> termMonths / 3;
            case SEMI_ANNUAL -> termMonths / 6;
            case ANNUAL -> termMonths / 12;
        };
    }

    public int monthsBetweenInstallments(Policy.PaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case SEMI_ANNUAL -> 6;
            case ANNUAL -> 12;
        };
    }

    public BigDecimal sumActiveCoverages(List<Policy> activePolicies) {
        return activePolicies.stream()
                .map(Policy::getCoverageAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildBreakdown(PolicyType pt, BigDecimal coverageFactor, BigDecimal ageFactor, Policy.PaymentFrequency frequency) {
        return "BasePremium=" + pt.getBasePremium()
                + ", CoverageFactor=" + coverageFactor
                + ", AgeFactor=" + ageFactor
                + ", Frequency=" + frequency.name();
    }
}
