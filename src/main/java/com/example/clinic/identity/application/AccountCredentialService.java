package com.example.clinic.identity.application;

import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Controlled account provisioning and credential lifecycle management service fulfilling WP-S04-06.
 * Serves as the single security boundary for password validation and encoding.
 * <p>
 * Security invariants:
 * <ul>
 *   <li>Plaintext credentials are only accepted at this controlled boundary and never persisted or logged.</li>
 *   <li>All hashes are produced via the configured {@link PasswordEncoder} (BCrypt strength 12).</li>
 *   <li>Pre-hashed values cannot be submitted as new plaintext to prevent double-hashing confusion.</li>
 *   <li>Password complexity enforces a minimum length of 8 and maximum length of 128 characters.</li>
 * </ul>
 */
@Service
@Transactional
public class AccountCredentialService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountCredentialService(UserAccountRepository userAccountRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new user account with securely encoded credentials.
     *
     * @param rawEmail    raw user email (will be normalized)
     * @param rawPassword plaintext password
     * @return persisted {@link UserAccount} with hash
     */
    public UserAccount createAccount(String rawEmail, String rawPassword) {
        return createAccount(rawEmail, rawPassword, true);
    }

    /**
     * Creates a new user account with initial enabled state.
     */
    public UserAccount createAccount(String rawEmail, String rawPassword, boolean enabled) {
        String normalizedEmail = validateAndNormalizeEmail(rawEmail);
        validateRawPassword(rawPassword);

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("User account with email already exists: " + normalizedEmail);
        }

        String encodedHash = passwordEncoder.encode(rawPassword);
        UserAccount account = new UserAccount(normalizedEmail, encodedHash);
        account.setEnabled(enabled);
        return userAccountRepository.save(account);
    }

    /**
     * Updates an account password after validating the caller's current password.
     */
    public void changePassword(UUID userId, String oldRawPassword, String newRawPassword) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(oldRawPassword, "Old password must not be null");
        validateRawPassword(newRawPassword);

        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found: " + userId));

        if (!account.isEnabled()) {
            throw new IllegalStateException("Cannot change password for disabled account: " + userId);
        }

        if (!passwordEncoder.matches(oldRawPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        String newHash = passwordEncoder.encode(newRawPassword);
        account.setPasswordHash(newHash);
    }

    /**
     * Administrative password reset boundary.
     */
    public void resetPassword(UUID userId, String newRawPassword) {
        Objects.requireNonNull(userId, "User ID must not be null");
        validateRawPassword(newRawPassword);

        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found: " + userId));

        String newHash = passwordEncoder.encode(newRawPassword);
        account.setPasswordHash(newHash);
    }

    public void disableAccount(UUID userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found: " + userId));
        account.setEnabled(false);
    }

    public void enableAccount(UUID userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found: " + userId));
        account.setEnabled(true);
    }

    public boolean verifyPassword(String rawPassword, String encodedHash) {
        if (rawPassword == null || encodedHash == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedHash);
    }

    private String validateAndNormalizeEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank");
        }
        String normalized = rawEmail.strip().toLowerCase(Locale.ROOT);
        if (!normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw new IllegalArgumentException("Malformed email address: " + rawEmail);
        }
        return normalized;
    }

    private void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be null, empty, or blank");
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must not exceed " + MAX_PASSWORD_LENGTH + " characters");
        }
        if (BCRYPT_PATTERN.matcher(rawPassword).matches()) {
            throw new IllegalArgumentException("Raw password cannot be a pre-computed BCrypt hash");
        }
    }
}
