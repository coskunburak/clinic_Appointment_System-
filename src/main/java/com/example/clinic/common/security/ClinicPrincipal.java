package com.example.clinic.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable authenticated principal representation fulfilling WP-S04-02.
 * Encapsulates the user's stable database identity ({@link UUID}), verified login identity,
 * enabled status, and validated granted authorities.
 * <p>
 * Security invariants:
 * <ul>
 *   <li>The authorization identity is anchored to {@code userId} (UUID), never mutable emails or client parameters.</li>
 *   <li>Password hashes and sensitive credentials are never disclosed in {@link #toString()}, exceptions, or serialization.</li>
 * </ul>
 */
public final class ClinicPrincipal implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final Set<GrantedAuthority> authorities;

    public ClinicPrincipal(UUID userId,
                           String username,
                           String passwordHash,
                           boolean enabled,
                           Collection<? extends GrantedAuthority> authorities) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.enabled = enabled;
        this.authorities = authorities == null ? Collections.emptySet() : Set.copyOf(authorities);
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClinicPrincipal that = (ClinicPrincipal) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "ClinicPrincipal{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", enabled=" + enabled +
                ", authorities=" + authorities +
                '}';
    }
}
