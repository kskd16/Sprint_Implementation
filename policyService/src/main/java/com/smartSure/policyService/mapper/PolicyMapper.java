package com.smartSure.policyService.mapper;

import com.smartSure.policyService.dto.policy.PolicyPurchaseRequest;
import com.smartSure.policyService.dto.policy.PolicyResponse;
import com.smartSure.policyService.dto.premium.PremiumResponse;
import com.smartSure.policyService.entity.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PremiumMapper.class, PolicyTypeMapper.class})
public interface PolicyMapper {

    @Mapping(target = "status", expression = "java(policy.getStatus().name())")
    @Mapping(target = "paymentFrequency", expression = "java(policy.getPaymentFrequency().name())")
    @Mapping(target = "policyType", source = "policy.policyType")
    @Mapping(target = "premiums", ignore = true)
    @Mapping(target = "createdAt", expression = "java(policy.getCreatedAt() != null ? policy.getCreatedAt().toString() : null)")
    PolicyResponse toResponse(Policy policy);

    @Mapping(target = "status", expression = "java(policy.getStatus().name())")
    @Mapping(target = "paymentFrequency", expression = "java(policy.getPaymentFrequency().name())")
    @Mapping(target = "policyType", source = "policy.policyType")
    @Mapping(target = "premiums", source = "premiums")
    @Mapping(target = "createdAt", expression = "java(policy.getCreatedAt() != null ? policy.getCreatedAt().toString() : null)")
    PolicyResponse toResponseWithPremiums(Policy policy, List<PremiumResponse> premiums);

    Policy toEntity(PolicyPurchaseRequest request);
}
