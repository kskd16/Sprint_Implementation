package com.smartSure.policyService.dto.policy;

import com.smartSure.policyService.entity.Policy;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PolicyStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private Policy.PolicyStatus status;

    private String reason;
}
