package com.smartSure.policyService.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "policy_types")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsuranceCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePremium;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxCoverageAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deductibleAmount;

    @Column(nullable = false)
    private Integer termMonths;

    private Integer minAge;
    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PolicyTypeStatus status = PolicyTypeStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String coverageDetails;

    @OneToMany(mappedBy = "policyType", fetch = FetchType.LAZY)
    private List<Policy> policies;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum InsuranceCategory {
        HEALTH, AUTO, HOME, LIFE, TRAVEL, BUSINESS
    }

    public enum PolicyTypeStatus {
        ACTIVE, INACTIVE, DISCONTINUED
    }
}
