package com.example.clinic.common.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fail-closed runtime safety guard protecting non-development environments and databases
 * from accidental execution of {@link DemoDataSeeder} or {@link DemoDataResetService}.
 */
@Component
public class DemoEnvironmentGuard {

    private static final String DEV_PROFILE = "dev";

    private static final Set<String> FORBIDDEN_PROFILES = Set.of(
            "prod", "production", "staging", "stage", "live", "preprod", "release"
    );

    private static final Set<String> SAFE_LOCAL_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "::1",
            "[::1]",
            "0:0:0:0:0:0:0:1",
            "host.docker.internal",
            "docker.for.mac.localhost",
            "docker.for.win.localhost",
            "postgres",
            "clinic-postgres",
            "clinic-db",
            "db",
            "testcontainers"
    );

    private static final Pattern UNSAFE_DB_TOKEN_PATTERN = Pattern.compile(
            "(^|[^a-z0-9])(prod|production|staging|preprod|live|rds\\.amazonaws\\.com|database\\.azure\\.com|supabase|cloudsql|neon\\.tech|render\\.com|elephantsql|aivencloud|cockroachlabs|heroku)([^a-z0-9]|$)"
    );

    private static final Pattern DOCKER_BRIDGE_IP_PATTERN = Pattern.compile(
            "^172\\.(1[6-9]|2\\d|3[0-1])\\.\\d{1,3}\\.\\d{1,3}$"
    );

    private final Environment environment;
    private final DemoDataProperties properties;
    private final ObjectProvider<DataSource> dataSourceProvider;

    public DemoEnvironmentGuard(Environment environment,
                                DemoDataProperties properties,
                                ObjectProvider<DataSource> dataSourceProvider) {
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.dataSourceProvider = Objects.requireNonNull(dataSourceProvider, "dataSourceProvider must not be null");
    }

    /**
     * Verifies that demo seeding is explicitly authorized in a verified local {@code dev} environment.
     *
     * @throws IllegalStateException if any environment, profile, opt-in flag, or database target check fails
     */
    public void assertSafeForSeeding() {
        assertDevProfileActive("demo seeding");
        boolean optedIn = properties.getSeed().isEnabled()
                || Boolean.parseBoolean(environment.getProperty("clinic.demo.seed.enabled", "false"));
        if (!optedIn) {
            throw new IllegalStateException(
                    "Refusing demo seeding: explicit property 'clinic.demo.seed.enabled=true' is required."
            );
        }
        assertSafeDatabaseTarget("demo seeding");
    }

    /**
     * Verifies that demo reset is explicitly authorized in a verified local {@code dev} environment.
     *
     * @throws IllegalStateException if any environment, profile, opt-in flag, or database target check fails
     */
    public void assertSafeForReset() {
        assertDevProfileActive("demo reset");
        boolean optedIn = properties.getReset().isEnabled()
                || Boolean.parseBoolean(environment.getProperty("clinic.demo.reset.enabled", "false"));
        if (!optedIn) {
            throw new IllegalStateException(
                    "Refusing demo reset: explicit property 'clinic.demo.reset.enabled=true' is required."
            );
        }
        assertSafeDatabaseTarget("demo reset");
    }

    private void assertDevProfileActive(String operation) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles == null || activeProfiles.length == 0) {
            throw new IllegalStateException(
                    "Refusing " + operation + ": no active Spring profile configured (explicit 'dev' profile required)."
            );
        }

        boolean hasDevProfile = false;
        for (String profile : activeProfiles) {
            if (profile == null || profile.isBlank()) {
                continue;
            }
            String normalized = profile.strip().toLowerCase(Locale.ROOT);
            if (FORBIDDEN_PROFILES.contains(normalized)) {
                throw new IllegalStateException(
                        "Refusing " + operation + ": forbidden active profile detected ('" + normalized + "')."
                );
            }
            if (DEV_PROFILE.equals(normalized)) {
                hasDevProfile = true;
            }
        }

        if (!hasDevProfile) {
            throw new IllegalStateException(
                    "Refusing " + operation + ": active Spring profiles " + Arrays.toString(activeProfiles)
                            + " do not include explicit 'dev' profile."
            );
        }
    }

    private void assertSafeDatabaseTarget(String operation) {
        String jdbcUrl = resolveJdbcUrl();
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException(
                    "Refusing " + operation + ": datasource JDBC URL is absent and cannot be verified as non-production."
            );
        }

        String normalized = jdbcUrl.strip().toLowerCase(Locale.ROOT);
        if (UNSAFE_DB_TOKEN_PATTERN.matcher(normalized).find()) {
            throw new IllegalStateException(
                    "Refusing " + operation + ": datasource URL contains forbidden production/remote target indicator."
            );
        }

        if (normalized.startsWith("jdbc:tc:postgresql:")) {
            return;
        }

        if (!normalized.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException(
                    "Refusing " + operation + ": datasource URL must target local PostgreSQL ('jdbc:postgresql://')."
            );
        }

        String withoutJdbc = normalized.substring("jdbc:".length());
        String host = extractHost(withoutJdbc);
        if (!isAllowedLocalHost(host)) {
            throw new IllegalStateException(
                    "Refusing " + operation + ": datasource host '" + host + "' is not an approved local development host."
            );
        }
    }

    private String resolveJdbcUrl() {
        String configuredUrl = environment.getProperty("spring.datasource.url");
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource instanceof HikariDataSource hikari) {
            return hikari.getJdbcUrl();
        }
        return null;
    }

    private String extractHost(String postgresqlUriString) {
        int queryIdx = postgresqlUriString.indexOf('?');
        String baseUri = queryIdx >= 0 ? postgresqlUriString.substring(0, queryIdx) : postgresqlUriString;

        // Handle PostgreSQL multi-host authority safely by inspecting every host token
        int schemeEnd = baseUri.indexOf("://");
        int slashIdx = baseUri.indexOf('/', schemeEnd + 3);
        String authority = slashIdx >= 0
                ? baseUri.substring(schemeEnd + 3, slashIdx)
                : baseUri.substring(schemeEnd + 3);

        if (authority.contains(",")) {
            for (String singleAuthority : authority.split(",")) {
                String candidate = parseSingleHost(singleAuthority);
                if (!isAllowedLocalHost(candidate)) {
                    return candidate;
                }
            }
            return parseSingleHost(authority.split(",")[0]);
        }

        return parseSingleHost(authority);
    }

    private String parseSingleHost(String authority) {
        if (authority == null || authority.isBlank()) {
            return "";
        }
        String trimmed = authority.strip();
        int atIdx = trimmed.lastIndexOf('@');
        if (atIdx >= 0) {
            trimmed = trimmed.substring(atIdx + 1);
        }
        if (trimmed.startsWith("[")) {
            int closingBracket = trimmed.indexOf(']');
            return closingBracket >= 0 ? trimmed.substring(0, closingBracket + 1) : trimmed;
        }
        try {
            URI parsed = URI.create("postgresql://" + trimmed + "/");
            if (parsed.getHost() != null && !parsed.getHost().isBlank()) {
                return parsed.getHost().toLowerCase(Locale.ROOT);
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to direct port splitting
        }
        int colonIdx = trimmed.indexOf(':');
        return (colonIdx >= 0 ? trimmed.substring(0, colonIdx) : trimmed).toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedLocalHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return SAFE_LOCAL_HOSTS.contains(normalized) || DOCKER_BRIDGE_IP_PATTERN.matcher(normalized).matches();
    }
}
