package com.example.clinic.identity.application;

import com.example.clinic.common.security.ClinicPrincipal;
import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Spring Security {@link UserDetailsService} adapter fulfilling WP-S04-05.
 * Authenticates users against {@link UserAccount} and resolves roles from persisted {@link Membership}
 * and {@code PatientProfile} records.
 * <p>
 * Security policies:
 * <ul>
 *   <li>Normalized email matching prevents casing and whitespace bypasses.</li>
 *   <li>Disabled accounts are rejected with {@link DisabledException}.</li>
 *   <li>Accounts without any legitimate memberships or patient profiles fail closed.</li>
 *   <li>Generic error messages prevent user enumeration.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ClinicUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final MembershipRepository membershipRepository;
    private final PatientProfileRepository patientProfileRepository;

    public ClinicUserDetailsService(UserAccountRepository userAccountRepository,
                                    MembershipRepository membershipRepository,
                                    PatientProfileRepository patientProfileRepository) {
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }

        String normalizedEmail = username.strip().toLowerCase(Locale.ROOT);
        UserAccount account = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        if (!account.isEnabled()) {
            throw new DisabledException("User account is disabled: " + normalizedEmail);
        }

        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Resolve authorities from explicit organization/branch memberships
        List<Membership> memberships = membershipRepository.findByUserAccountId(account.getId());
        for (Membership membership : memberships) {
            Role role = membership.getRole();
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority(role.authority()));
            }
        }

        // 2. Resolve patient profile role
        if (patientProfileRepository.findByUserAccountId(account.getId()).isPresent()) {
            authorities.add(new SimpleGrantedAuthority(Role.ROLE_PATIENT.authority()));
        }

        // 3. Strict fail-closed policy: reject accounts without any valid role
        if (authorities.isEmpty()) {
            throw new UsernameNotFoundException("Invalid credentials: no authorized roles associated with account");
        }

        return new ClinicPrincipal(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.isEnabled(),
                authorities
        );
    }
}
