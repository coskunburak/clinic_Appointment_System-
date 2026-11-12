package com.example.clinic.web.common;

import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collection;
import java.util.Optional;

/**
 * Global presentation model advice populating authoritative authentication and role metadata
 * to eliminate template-level security dialect dependencies and guarantee offline presentation robustness.
 */
@ControllerAdvice
public class GlobalModelAttributesAdvice {

    private final SecurityContextFacade securityContextFacade;

    public GlobalModelAttributesAdvice(SecurityContextFacade securityContextFacade) {
        this.securityContextFacade = securityContextFacade;
    }

    @ModelAttribute
    public void populateSecurityAttributes(Model model, HttpServletRequest request) {
        Optional<UserAccount> currentUserOpt = securityContextFacade.getCurrentUser();
        if (currentUserOpt.isPresent()) {
            UserAccount user = currentUserOpt.get();
            model.addAttribute("currentUser", user);
            model.addAttribute("currentUsername", user.getEmail());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
                boolean isAdmin = authorities.stream().anyMatch(a -> Role.ROLE_ADMIN.authority().equals(a.getAuthority()));
                boolean isStaff = authorities.stream().anyMatch(a -> Role.ROLE_STAFF.authority().equals(a.getAuthority()));
                boolean isPatient = authorities.stream().anyMatch(a -> Role.ROLE_PATIENT.authority().equals(a.getAuthority()));

                model.addAttribute("isAdmin", isAdmin);
                model.addAttribute("isStaff", isStaff);
                model.addAttribute("isPatient", isPatient);

                if (isAdmin) {
                    model.addAttribute("currentRole", "ROLE_ADMIN");
                    model.addAttribute("roleDisplayName", "Yönetici");
                } else if (isStaff) {
                    model.addAttribute("currentRole", "ROLE_STAFF");
                    model.addAttribute("roleDisplayName", "Sağlık Personeli / Hekim");
                } else if (isPatient) {
                    model.addAttribute("currentRole", "ROLE_PATIENT");
                    model.addAttribute("roleDisplayName", "Hasta");
                }
            }
        } else {
            model.addAttribute("isAdmin", false);
            model.addAttribute("isStaff", false);
            model.addAttribute("isPatient", false);
        }

        if (request != null) {
            model.addAttribute("currentUri", request.getRequestURI());
        }
    }
}
