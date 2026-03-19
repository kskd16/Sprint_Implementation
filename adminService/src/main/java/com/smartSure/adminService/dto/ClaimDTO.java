package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDTO {
    private Long id;
    private Long userId;
    private Long policyId;
    private String description;
    private BigDecimal claimAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
