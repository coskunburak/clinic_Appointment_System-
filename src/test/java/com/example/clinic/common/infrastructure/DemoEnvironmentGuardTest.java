package com.example.clinic.common.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoEnvironmentGuardTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<DataSource> emptyDataSourceProvider = mock(ObjectProvider.class);

    @Test
    @DisplayName("demoSeederOnlyRunsInDevProfile: rejects empty, default, test, prod, and staging profiles")
    void demoSeederOnlyRunsInDevProfile() {
        DemoDataProperties props = devSeedEnabledProps();

        MockEnvironment defaultEnv = new MockEnvironment()
                .withProperty("clinic.demo.seed.enabled", "true")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/clinic_db");
        assertThatThrownBy(() -> new DemoEnvironmentGuard(defaultEnv, props, emptyDataSourceProvider).assertSafeForSeeding())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no active Spring profile");

        for (String profile : new String[]{"test", "prod", "production", "staging", "live"}) {
            MockEnvironment env = new MockEnvironment()
                    .withProperty("clinic.demo.seed.enabled", "true")
                    .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/clinic_db");
            env.setActiveProfiles(profile);

            assertThatThrownBy(() -> new DemoEnvironmentGuard(env, props, emptyDataSourceProvider).assertSafeForSeeding())
                    .as("Expected profile '%s' to be rejected", profile)
                    .isInstanceOf(IllegalStateException.class);
        }

        MockEnvironment mixedDevProdEnv = new MockEnvironment()
                .withProperty("clinic.demo.seed.enabled", "true")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/clinic_db");
        mixedDevProdEnv.setActiveProfiles("dev", "prod");
        assertThatThrownBy(() -> new DemoEnvironmentGuard(mixedDevProdEnv, props, emptyDataSourceProvider).assertSafeForSeeding())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forbidden active profile");

        MockEnvironment devEnv = safeDevSeedEnv();
        assertThatCode(() -> new DemoEnvironmentGuard(devEnv, props, emptyDataSourceProvider).assertSafeForSeeding())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("demoSeederRejectsUnsafeDatabaseConfiguration: rejects remote hosts, prod db names, non-PostgreSQL, and missing URL")
    void demoSeederRejectsUnsafeDatabaseConfiguration() {
        DemoDataProperties props = devSeedEnabledProps();

        for (String unsafeUrl : new String[]{
                "",
                "jdbc:h2:mem:clinic",
                "jdbc:mysql://localhost:3306/clinic_db",
                "jdbc:postgresql://clinic-db.example.com:5432/clinic_db",
                "jdbc:postgresql://mycluster.abc.us-east-1.rds.amazonaws.com:5432/clinic_db",
                "jdbc:postgresql://db.neon.tech:5432/clinic_db",
                "jdbc:postgresql://localhost:5432/clinic_prod",
                "jdbc:postgresql://127.0.0.1:5432/clinic_production",
                "jdbc:postgresql://localhost:5432/clinic_staging",
                "jdbc:postgresql://localhost:5432/live_clinic_db",
                "jdbc:postgresql://localhost:5432,remote.example.com:5432/clinic_db"
        }) {
            MockEnvironment env = new MockEnvironment()
                    .withProperty("clinic.demo.seed.enabled", "true")
                    .withProperty("spring.datasource.url", unsafeUrl);
            env.setActiveProfiles("dev");

            assertThatThrownBy(() -> new DemoEnvironmentGuard(env, props, emptyDataSourceProvider).assertSafeForSeeding())
                    .as("Expected JDBC URL '%s' to be rejected", unsafeUrl)
                    .isInstanceOf(IllegalStateException.class);
        }

        for (String safeUrl : new String[]{
                "jdbc:postgresql://localhost:5433/clinic_db",
                "jdbc:postgresql://127.0.0.1:5433/clinic_test_db?loggerLevel=OFF",
                "jdbc:postgresql://[::1]:5432/clinic_db",
                "jdbc:postgresql://postgres:5432/clinic_db",
                "jdbc:postgresql://172.18.0.2:5432/clinic_db",
                "jdbc:tc:postgresql:17-alpine:///clinic_test_db"
        }) {
            MockEnvironment env = new MockEnvironment()
                    .withProperty("clinic.demo.seed.enabled", "true")
                    .withProperty("spring.datasource.url", safeUrl);
            env.setActiveProfiles("dev");

            assertThatCode(() -> new DemoEnvironmentGuard(env, props, emptyDataSourceProvider).assertSafeForSeeding())
                    .as("Expected safe local URL '%s' to be accepted", safeUrl)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Fallback HikariDataSource URL resolution still enforces non-production DB checks")
    @SuppressWarnings("unchecked")
    void resolvesJdbcUrlFromHikariFallback() {
        DemoDataProperties props = devSeedEnabledProps();
        MockEnvironment env = new MockEnvironment()
                .withProperty("clinic.demo.seed.enabled", "true");
        env.setActiveProfiles("dev");

        HikariDataSource hikari = mock(HikariDataSource.class);
        when(hikari.getJdbcUrl()).thenReturn("jdbc:postgresql://rds.amazonaws.com:5432/clinic_db");
        ObjectProvider<DataSource> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(hikari);

        assertThatThrownBy(() -> new DemoEnvironmentGuard(env, props, provider).assertSafeForSeeding())
                .isInstanceOf(IllegalStateException.class);

        when(hikari.getJdbcUrl()).thenReturn("jdbc:postgresql://127.0.0.1:5433/clinic_db");
        assertThatCode(() -> new DemoEnvironmentGuard(env, props, provider).assertSafeForSeeding())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("demoResetIsDisabledOutsideDev: reset requires both dev profile and explicit opt-in flag")
    void demoResetRequiresDevProfileAndExplicitOptIn() {
        DemoDataProperties props = new DemoDataProperties();
        MockEnvironment devEnvNoOptIn = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/clinic_db")
                .withProperty("clinic.demo.reset.enabled", "false");
        devEnvNoOptIn.setActiveProfiles("dev");

        assertThatThrownBy(() -> new DemoEnvironmentGuard(devEnvNoOptIn, props, emptyDataSourceProvider).assertSafeForReset())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clinic.demo.reset.enabled=true");

        MockEnvironment prodEnvWithOptIn = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/clinic_db")
                .withProperty("clinic.demo.reset.enabled", "true");
        prodEnvWithOptIn.setActiveProfiles("prod");
        props.getReset().setEnabled(true);

        assertThatThrownBy(() -> new DemoEnvironmentGuard(prodEnvWithOptIn, props, emptyDataSourceProvider).assertSafeForReset())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forbidden active profile");

        MockEnvironment devEnvWithOptIn = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/clinic_db")
                .withProperty("clinic.demo.reset.enabled", "true");
        devEnvWithOptIn.setActiveProfiles("dev");

        assertThatCode(() -> new DemoEnvironmentGuard(devEnvWithOptIn, props, emptyDataSourceProvider).assertSafeForReset())
                .doesNotThrowAnyException();
    }

    private DemoDataProperties devSeedEnabledProps() {
        DemoDataProperties props = new DemoDataProperties();
        props.getSeed().setEnabled(true);
        return props;
    }

    private MockEnvironment safeDevSeedEnv() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("clinic.demo.seed.enabled", "true")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/clinic_db");
        env.setActiveProfiles("dev");
        return env;
    }
}
