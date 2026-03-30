package com.smartSure.policyService.service;

import com.smartSure.policyService.dto.calculation.PremiumCalculationResponse;
import com.smartSure.policyService.entity.Policy;
import com.smartSure.policyService.entity.PolicyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PremiumCalculator.
 * Tests premium calculation logic for different ages, coverage amounts, and frequencies.
 */
class PremiumCalculatorTest {

    private PremiumCalculator calculator;
    private PolicyType policyType;

    @BeforeEach
    void setUp() {
        calculator = new PremiumCalculator();

        policyType = new PolicyType();
        policyType.setBasePremium(new BigDecimal("5000.00"));
        policyType.setMaxCoverageAmount(new BigDecimal("1000000.00"));
        policyType.setTermMonths(12);
    }

    @Test
    @DisplayName("Calculate premium - annual frequency returns correct amount")
    void calculatePremium_annualFrequency() {
        PremiumCalculationResponse response = calculator.calculatePremium(
                policyType,
                new BigDecimal("500000.00"),
                Policy.PaymentFrequency.ANNUAL,
                30
        );

        assertThat(response).isNotNull();
        assertThat(response.getCalculatedPremium()).isPositive();
        assertThat(response.getAnnualPremium()).isEqualTo(response.getCalculatedPremium());
    }

    @Test
    @DisplayName("Calculate premium - monthly frequency is higher than annual per installment")
    void calculatePremium_monthlyHigherThanAnnualPerInstallment() {
        PremiumCalculationResponse monthly = calculator.calculatePremium(
                policyType, new BigDecimal("500000.00"), Policy.PaymentFrequency.MONTHLY, 30);
        PremiumCalculationResponse annual = calculator.calculatePremium(
                policyType, new BigDecimal("500000.00"), Policy.PaymentFrequency.ANNUAL, 30);

        // Monthly per installment * 12 should be > annual (due to 5% loading)
        BigDecimal monthlyTotal = monthly.getCalculatedPremium().multiply(new BigDecimal("12"));
        assertThat(monthlyTotal).isGreaterThan(annual.getCalculatedPremium());
    }

    @Test
    @DisplayName("Calculate premium - older customer pays more")
    void calculatePremium_olderCustomerPaysMore() {
        PremiumCalculationResponse young = calculator.calculatePremium(
                policyType, new BigDecimal("500000.00"), Policy.PaymentFrequency.ANNUAL, 25);
        PremiumCalculationResponse old = calculator.calculatePremium(
                policyType, new BigDecimal("500000.00"), Policy.PaymentFrequency.ANNUAL, 60);

        assertThat(old.getCalculatedPremium()).isGreaterThan(young.getCalculatedPremium());
    }

    @Test
    @DisplayName("Calculate premium - higher coverage means higher premium")
    void calculatePremium_higherCoverageHigherPremium() {
        PremiumCalculationResponse low = calculator.calculatePremium(
                policyType, new BigDecimal("100000.00"), Policy.PaymentFrequency.ANNUAL, 30);
        PremiumCalculationResponse high = calculator.calculatePremium(
                policyType, new BigDecimal("500000.00"), Policy.PaymentFrequency.ANNUAL, 30);

        assertThat(high.getCalculatedPremium()).isGreaterThan(low.getCalculatedPremium());
    }

    @Test
    @DisplayName("Installment count - monthly for 12 months = 12 installments")
    void installmentCount_monthlyFor12Months() {
        int count = calculator.installmentCount(12, Policy.PaymentFrequency.MONTHLY);
        assertThat(count).isEqualTo(12);
    }

    @Test
    @DisplayName("Installment count - annual for 12 months = 1 installment")
    void installmentCount_annualFor12Months() {
        int count = calculator.installmentCount(12, Policy.PaymentFrequency.ANNUAL);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Installment count - quarterly for 12 months = 4 installments")
    void installmentCount_quarterlyFor12Months() {
        int count = calculator.installmentCount(12, Policy.PaymentFrequency.QUARTERLY);
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("Calculate premium - null age uses factor of 1.0")
    void calculatePremium_nullAgeUsesDefaultFactor() {
        PremiumCalculationResponse withAge = calculator.calculatePremium(
                policyType, new BigDecimal("500000.00"), Policy.PaymentFrequency.ANNUAL, 30);
        PremiumCalculationResponse withoutAge = calculator.calculatePremium(
                policyType, new BigDecimal("500000.00"), Policy.PaymentFrequency.ANNUAL, null);

        // Age 30 has factor 1.0, same as null
        assertThat(withAge.getCalculatedPremium()).isEqualByComparingTo(withoutAge.getCalculatedPremium());
    }
}
