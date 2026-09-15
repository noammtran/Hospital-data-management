package com.usth.hospital.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    private static final String SYSTEM_USER = "SYSTEM";

    @Override
    public String username() {
        Authentication authentication = authentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_USER;
        }
        return authentication.getName();
    }

    @Override
    public CurrentUserSnapshot snapshot() {
        Authentication authentication = authentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new CurrentUserSnapshot(SYSTEM_USER, SYSTEM_USER, null);
        }
        if (authentication.getDetails() instanceof CurrentUserSnapshot snapshot) {
            return snapshot;
        }
        return new CurrentUserSnapshot(authentication.getName(), authentication.getName(), null);
    }

    private static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
