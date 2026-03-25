package com.smartSure.claimService.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PolicyDTO {
    private long policyID;
    private BigDecimal amount;
    private long userId;
}
