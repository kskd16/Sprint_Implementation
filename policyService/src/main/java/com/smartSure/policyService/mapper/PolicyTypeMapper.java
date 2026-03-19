package com.smartSure.policyService.mapper;

import com.smartSure.policyService.dto.policytype.PolicyTypeResponse;
import com.smartSure.policyService.entity.PolicyType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PolicyTypeMapper {

    @Mapping(target = "category", expression = "java(pt.getCategory().name())")
    @Mapping(target = "status", expression = "java(pt.getStatus().name())")
    @Mapping(target = "createdAt", expression = "java(pt.getCreatedAt() != null ? pt.getCreatedAt().toString() : null)")
    PolicyTypeResponse toResponse(PolicyType pt);
}
