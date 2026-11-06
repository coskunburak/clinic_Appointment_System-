package com.example.clinic.common.security;

import com.example.clinic.identity.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Deterministic role-based redirect handler for successful form authentication.
 * Fulfills WP-S04-04 login flow specifications.
 * <p>
 * Priority order:
 * <ol>
 *   <li>{@code ROLE_ADMIN} -> {@code /admin/dashboard}</li>
 *   <li>{@code ROLE_STAFF} -> {@code /staff/dashboard}</li>
 *   <li>{@code ROLE_PATIENT} -> {@code /patient/dashboard}</li>
 *   <li>Fallback -> {@code /}</li>
 * </ol>
 */
@Component
public class ClinicAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        if (hasAuthority(authorities, Role.ROLE_ADMIN.authority())) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            return;
        }

        if (hasAuthority(authorities, Role.ROLE_STAFF.authority())) {
            response.sendRedirect(request.getContextPath() + "/staff/dashboard");
            return;
        }

        if (hasAuthority(authorities, Role.ROLE_PATIENT.authority())) {
            response.sendRedirect(request.getContextPath() + "/patient/dashboard");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/");
    }

    private boolean hasAuthority(Collection<? extends GrantedAuthority> authorities, String roleName) {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(roleName));
    }
}
