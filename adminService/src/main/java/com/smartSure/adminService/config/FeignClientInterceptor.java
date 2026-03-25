package com.smartSure.adminService.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Value("${internal.secret}")
    private String internalSecret;

    @Override
    public void apply(RequestTemplate template) {
        // Always forward internal secret — required by claimService and authService InternalRequestFilter
        template.header("X-Internal-Secret", internalSecret);

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // Forward JWT for services that validate it
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                template.header("Authorization", authHeader);
            }

            // Forward gateway identity headers — required by claimService HeaderAuthenticationFilter
            String userId = request.getHeader("X-User-Id");
            String role   = request.getHeader("X-User-Role");
            if (userId != null) template.header("X-User-Id", userId);
            if (role != null)   template.header("X-User-Role", role);
        }
    }
}
