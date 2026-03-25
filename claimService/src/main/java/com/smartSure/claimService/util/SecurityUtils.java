package com.smartSure.claimService.util;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    // Principal is set as String (userId from X-User-Id header)
    public static String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static String getCurrentRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .iterator()
                .next().getAuthority();
    }
}
