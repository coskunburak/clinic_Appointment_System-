package com.example.clinic.identity.application;

import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountCredentialServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private PasswordEncoder passwordEncoder;
    private AccountCredentialService credentialService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        credentialService = new AccountCredentialService(userAccountRepository, passwordEncoder);
    }

    @Test
    @DisplayName("TC-025 / FR-025: Should hash password with BCrypt (strength 12) and persist account")
    void shouldHashPasswordWithBCrypt() {
        when(userAccountRepository.existsByEmail("user@clinic.test")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount account = credentialService.createAccount("user@clinic.test", "SecurePass123!");

        assertThat(account.getEmail()).isEqualTo("user@clinic.test");
        assertThat(account.isEnabled()).isTrue();

        // Must be valid BCrypt format ($2a$12$...)
        assertThat(account.getPasswordHash()).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches("SecurePass123!", account.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", account.getPasswordHash())).isFalse();

        // Plaintext must never leak in toString
        assertThat(account.toString()).doesNotContain("SecurePass123!");
    }

    @Test
    @DisplayName("Should normalize email during account creation")
    void shouldNormalizeEmail() {
        when(userAccountRepository.existsByEmail("user@clinic.test")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount account = credentialService.createAccount("  USER@Clinic.TEST  ", "SecurePass123!");

        assertThat(account.getEmail()).isEqualTo("user@clinic.test");
    }

    @Test
    @DisplayName("Negative: should reject duplicate email registration")
    void shouldRejectDuplicateEmail() {
        when(userAccountRepository.existsByEmail("duplicate@clinic.test")).thenReturn(true);

        assertThatThrownBy(() -> credentialService.createAccount("duplicate@clinic.test", "SecurePass123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userAccountRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "12", "short", "1234567"})
    @DisplayName("Negative: should reject password shorter than 8 characters")
    void shouldRejectShortPassword(String shortPass) {
        assertThatThrownBy(() -> credentialService.createAccount("user@clinic.test", shortPass))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("Negative: should reject blank or empty password")
    void shouldRejectBlankPassword(String blankPass) {
        assertThatThrownBy(() -> credentialService.createAccount("user@clinic.test", blankPass))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null, empty, or blank");
    }

    @Test
    @DisplayName("Negative: should reject pre-computed BCrypt hash submitted as plaintext")
    void shouldRejectPrecomputedBcryptHash() {
        String fakeHash = "$2a$12$e80yq9g3FvBskF5R3GjZleB0t7zXNlGsqZ6kHkQ.8JvC6I9s4sH2m";
        assertThatThrownBy(() -> credentialService.createAccount("user@clinic.test", fakeHash))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be a pre-computed BCrypt hash");
    }

    @Test
    @DisplayName("Should change password with valid current password")
    void shouldChangePasswordWithValidCurrentPassword() {
        UUID userId = UUID.randomUUID();
        String initialHash = passwordEncoder.encode("OldPassword123!");
        UserAccount account = new UserAccount(userId, "user@clinic.test", initialHash, true, java.time.Instant.now());

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        credentialService.changePassword(userId, "OldPassword123!", "NewPassword456!");

        assertThat(passwordEncoder.matches("NewPassword456!", account.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("OldPassword123!", account.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("Negative: should reject password change if current password does not match")
    void shouldRejectPasswordChangeWithWrongCurrentPassword() {
        UUID userId = UUID.randomUUID();
        String initialHash = passwordEncoder.encode("OldPassword123!");
        UserAccount account = new UserAccount(userId, "user@clinic.test", initialHash, true, java.time.Instant.now());

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> credentialService.changePassword(userId, "WrongPassword!", "NewPassword456!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password does not match");

        // Hash must remain unchanged
        assertThat(account.getPasswordHash()).isEqualTo(initialHash);
    }

    @Test
    @DisplayName("Negative: should reject password change for disabled account")
    void shouldRejectPasswordChangeForDisabledAccount() {
        UUID userId = UUID.randomUUID();
        String initialHash = passwordEncoder.encode("OldPassword123!");
        UserAccount account = new UserAccount(userId, "user@clinic.test", initialHash, false, java.time.Instant.now());

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> credentialService.changePassword(userId, "OldPassword123!", "NewPassword456!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change password for disabled account");
    }

    @Test
    @DisplayName("Should reset password administratively")
    void shouldResetPasswordAdministratively() {
        UUID userId = UUID.randomUUID();
        String initialHash = passwordEncoder.encode("OldPassword123!");
        UserAccount account = new UserAccount(userId, "user@clinic.test", initialHash, true, java.time.Instant.now());

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        credentialService.resetPassword(userId, "AdminResetPass789!");

        assertThat(passwordEncoder.matches("AdminResetPass789!", account.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Should enable and disable accounts correctly")
    void shouldEnableAndDisableAccount() {
        UUID userId = UUID.randomUUID();
        UserAccount account = new UserAccount(userId, "user@clinic.test", "hash", true, java.time.Instant.now());

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        credentialService.disableAccount(userId);
        assertThat(account.isEnabled()).isFalse();

        credentialService.enableAccount(userId);
        assertThat(account.isEnabled()).isTrue();
    }
}
