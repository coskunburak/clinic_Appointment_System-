package com.example.clinic.identity.application;

import com.example.clinic.common.security.ClinicPrincipal;
import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @InjectMocks
    private ClinicUserDetailsService userDetailsService;

    private Organization org;
    private Branch branch;

    @BeforeEach
    void setUp() {
        org = new Organization("Test Org");
        branch = new Branch(org, "Test Branch", "Europe/Istanbul", null);
    }

    @Test
    @DisplayName("Should authenticate valid user and map membership to Spring Security authority")
    void shouldAuthenticateValidUserWithMembership() {
        UserAccount account = new UserAccount("admin@clinic.test", "$2a$12$hashedPassword");
        Membership membership = new Membership(account, org, null, Role.ROLE_ADMIN);

        when(userAccountRepository.findByEmail("admin@clinic.test")).thenReturn(Optional.of(account));
        when(membershipRepository.findByUserAccountId(account.getId())).thenReturn(List.of(membership));
        when(patientProfileRepository.findByUserAccountId(account.getId())).thenReturn(Optional.empty());

        UserDetails details = userDetailsService.loadUserByUsername("admin@clinic.test");

        assertThat(details).isInstanceOf(ClinicPrincipal.class);
        ClinicPrincipal principal = (ClinicPrincipal) details;
        assertThat(principal.getUserId()).isEqualTo(account.getId());
        assertThat(principal.getUsername()).isEqualTo("admin@clinic.test");
        assertThat(principal.getPassword()).isEqualTo("$2a$12$hashedPassword");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should normalize email with whitespace and uppercase")
    void shouldNormalizeEmail() {
        UserAccount account = new UserAccount("admin@clinic.test", "$2a$12$hashedPassword");
        Membership membership = new Membership(account, org, null, Role.ROLE_ADMIN);

        when(userAccountRepository.findByEmail("admin@clinic.test")).thenReturn(Optional.of(account));
        when(membershipRepository.findByUserAccountId(account.getId())).thenReturn(List.of(membership));
        when(patientProfileRepository.findByUserAccountId(account.getId())).thenReturn(Optional.empty());

        UserDetails details = userDetailsService.loadUserByUsername("  ADMIN@Clinic.Test  ");

        assertThat(details.getUsername()).isEqualTo("admin@clinic.test");
    }

    @Test
    @DisplayName("Should resolve patient role from patient profile")
    void shouldResolvePatientRoleFromProfile() {
        UserAccount account = new UserAccount("patient@clinic.test", "$2a$12$hashedPassword");
        PatientProfile profile = new PatientProfile(account, "Ali Veli", "+905551112233");

        when(userAccountRepository.findByEmail("patient@clinic.test")).thenReturn(Optional.of(account));
        when(membershipRepository.findByUserAccountId(account.getId())).thenReturn(List.of());
        when(patientProfileRepository.findByUserAccountId(account.getId())).thenReturn(Optional.of(profile));

        UserDetails details = userDetailsService.loadUserByUsername("patient@clinic.test");

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PATIENT");
    }

    @Test
    @DisplayName("Should aggregate multiple legitimate roles for multi-role actor")
    void shouldAggregateMultiRoles() {
        UserAccount account = new UserAccount("doctor_admin@clinic.test", "$2a$12$hashedPassword");
        Membership staffMembership = new Membership(account, org, branch, Role.ROLE_STAFF);
        Membership adminMembership = new Membership(account, org, null, Role.ROLE_ADMIN);

        when(userAccountRepository.findByEmail("doctor_admin@clinic.test")).thenReturn(Optional.of(account));
        when(membershipRepository.findByUserAccountId(account.getId()))
                .thenReturn(List.of(staffMembership, adminMembership));
        when(patientProfileRepository.findByUserAccountId(account.getId())).thenReturn(Optional.empty());

        UserDetails details = userDetailsService.loadUserByUsername("doctor_admin@clinic.test");

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_STAFF", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Negative: nonexistent user must throw UsernameNotFoundException without leaking existence")
    void shouldRejectNonexistentUser() {
        when(userAccountRepository.findByEmail("ghost@clinic.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost@clinic.test"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("Negative: disabled account must throw DisabledException")
    void shouldRejectDisabledAccount() {
        UserAccount account = new UserAccount("disabled@clinic.test", "$2a$12$hashedPassword");
        account.setEnabled(false);

        when(userAccountRepository.findByEmail("disabled@clinic.test")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("disabled@clinic.test"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("Negative: account with no memberships and no profile must fail closed")
    void shouldFailClosedWhenAccountHasNoRoles() {
        UserAccount account = new UserAccount("empty@clinic.test", "$2a$12$hashedPassword");

        when(userAccountRepository.findByEmail("empty@clinic.test")).thenReturn(Optional.of(account));
        when(membershipRepository.findByUserAccountId(account.getId())).thenReturn(List.of());
        when(patientProfileRepository.findByUserAccountId(account.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("empty@clinic.test"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("no authorized roles");
    }
}
