package com.example.clinic.common.security;

import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Security context bridge providing secure access to the currently authenticated actor.
 * Resolves the persistent {@link UserAccount} from {@link ClinicPrincipal} or Spring Security context.
 */
@Component
public class SecurityContextFacade {

    private final UserAccountRepository userAccountRepository;

    public SecurityContextFacade(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Resolves the current authenticated user account, or returns empty if unauthenticated or anonymous.
     */
    public Optional<UserAccount> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof ClinicPrincipal clinicPrincipal) {
            return userAccountRepository.findById(clinicPrincipal.getUserId());
        }
        if (principal instanceof UserDetails userDetails) {
            return userAccountRepository.findByEmail(userDetails.getUsername().strip().toLowerCase(Locale.ROOT));
        }
        if (principal instanceof String username && !username.isBlank()) {
            return userAccountRepository.findByEmail(username.strip().toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    /**
     * Returns the current authenticated user account or throws {@link AccessDeniedException} if unauthenticated.
     */
    public UserAccount getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication required: no authenticated user in security context"));
    }
}
