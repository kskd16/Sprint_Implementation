package com.smartSure.policyService.mapper;

import com.smartSure.policyService.dto.premium.PremiumResponse;
import com.smartSure.policyService.entity.Premium;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PremiumMapper {

    @Mapping(target = "status", expression = "java(premium.getStatus().name())")
    @Mapping(target = "paymentMethod", expression = "java(premium.getPaymentMethod() != null ? premium.getPaymentMethod().name() : null)")
    PremiumResponse toResponse(Premium premium);
}
