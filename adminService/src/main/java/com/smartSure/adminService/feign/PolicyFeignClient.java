package com.smartSure.adminService.feign;

import com.smartSure.adminService.dto.PolicyDTO;
import com.smartSure.adminService.dto.PolicyStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client for PolicyService.
 * Authorization header is forwarded automatically by FeignClientInterceptor.
 * No X-User-Id or X-User-Role headers needed — JWT principal is used.
 */
@FeignClient(name = "policyService")
public interface PolicyFeignClient {

    @GetMapping("/api/policies/admin/all")
    List<PolicyDTO> getAllPolicies();

    @GetMapping("/api/policies/{policyId}")
    PolicyDTO getPolicyById(@PathVariable Long policyId);

    @PutMapping("/api/policies/admin/{policyId}/status")
    PolicyDTO updatePolicyStatus(
            @PathVariable Long policyId,
            @RequestBody PolicyStatusUpdateRequest request
    );
}
