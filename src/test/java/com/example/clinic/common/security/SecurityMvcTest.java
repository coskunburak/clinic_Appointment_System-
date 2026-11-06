package com.example.clinic.common.security;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice & security filter chain tests fulfilling Phase 9.B, NFR-001, and TC-037.
 * Validates authentication, role route restrictions, CSRF enforcement, and session invalidation.
 */
@AutoConfigureMockMvc
@Transactional
class SecurityMvcTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserAccount adminUser;
    private UserAccount disabledUser;

    @BeforeEach
    void setUpFixtures() {
        Organization org = organizationRepository.saveAndFlush(new Organization("MVC Test Org"));

        String rawPassword = "Password123!";
        String encodedHash = passwordEncoder.encode(rawPassword);

        adminUser = userAccountRepository.saveAndFlush(
                new UserAccount("mvc_admin@clinic.test", encodedHash)
        );
        membershipRepository.saveAndFlush(new Membership(adminUser, org, null, Role.ROLE_ADMIN));

        disabledUser = new UserAccount("mvc_disabled@clinic.test", encodedHash);
        disabledUser.setEnabled(false);
        userAccountRepository.saveAndFlush(disabledUser);
        membershipRepository.saveAndFlush(new Membership(disabledUser, org, null, Role.ROLE_ADMIN));
    }

    @Test
    @DisplayName("shouldAuthenticateValidUser: Valid credentials authenticate and redirect to authorized dashboard")
    void shouldAuthenticateValidUser() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", "mvc_admin@clinic.test")
                .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("shouldRejectInvalidPassword: Bad password fails authentication and redirects to /login?error")
    void shouldRejectInvalidPassword() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", "mvc_admin@clinic.test")
                .param("password", "WrongPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("shouldRejectDisabledAccount: Disabled user is rejected and cannot authenticate")
    void shouldRejectDisabledAccount() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", "mvc_disabled@clinic.test")
                .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("shouldRedirectUnauthenticatedUserToLogin: Unauthenticated request to protected route redirects to login")
    void shouldRedirectUnauthenticatedUserToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("patientCannotAccessAdminEndpoints: Patient receives 403 Forbidden on /admin/** routes")
    void patientCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                .with(user("patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("staffCannotAccessAdminEndpoints: Staff receives 403 Forbidden on /admin/** routes")
    void staffCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                .with(user("doctor@clinic.test").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("adminCannotAccessPatientOnlyEndpoints: Admin receives 403 Forbidden on /patient/** routes")
    void adminCannotAccessPatientOnlyEndpoints() throws Exception {
        mockMvc.perform(get("/patient/dashboard")
                .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("csrfTokenIsRequiredOnPostRequests: POST request without CSRF token returns 403 Forbidden")
    void csrfTokenIsRequiredOnPostRequests() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldAllowPostWithValidCsrfWhenAuthorized: POST with valid CSRF succeeds and logs out")
    void shouldAllowPostWithValidCsrfWhenAuthorized() throws Exception {
        mockMvc.perform(post("/logout")
                .with(csrf())
                .with(user("admin@clinic.test").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @DisplayName("shouldInvalidateSessionOnLogout: Logout clears security context and invalidates session")
    void shouldInvalidateSessionOnLogout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/logout")
                .with(csrf())
                .with(user("admin@clinic.test").roles("ADMIN"))
                .session(session))
                .andExpect(status().is3xxRedirection());

        assertThat(session.isInvalid()).isTrue();
    }
}
