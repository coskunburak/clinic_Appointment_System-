package com.example.clinic.common.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DemoDataSeederUnitTest {

    @Test
    @DisplayName("Default application.yml never activates dev profile or demo seeding/reset by default")
    void defaultConfigurationDisablesDemoSeedAndReset() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties props = yaml.getObject();

        assertThat(props).isNotNull();
        assertThat(props.getProperty("spring.profiles.default")).isNull();
        assertThat(props.getProperty("clinic.demo.seed.enabled")).isEqualTo("false");
        assertThat(props.getProperty("clinic.demo.reset.enabled")).isEqualTo("false");
        assertThat(props.getProperty("clinic.demo.reset.reseed")).isEqualTo("false");
    }

    @Test
    @DisplayName("DemoDataSeeder is strictly restricted to dev profile and explicit property condition")
    void seederCarriesDevProfileAndOptInAnnotations() {
        Profile profile = DemoDataSeeder.class.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("dev");

        ConditionalOnExpression condition = DemoDataSeeder.class.getAnnotation(ConditionalOnExpression.class);
        assertThat(condition).isNotNull();
        assertThat(condition.value()).contains("clinic.demo.seed.enabled:false");
        assertThat(condition.value()).contains("clinic.demo.reset.enabled:false");
    }

    @Test
    @DisplayName("DemoDataProperties redacts plaintext secrets in toString output")
    void propertiesRedactCredentialsInLogs() {
        DemoDataProperties props = new DemoDataProperties();
        props.getCredentials().setAdminPassword("AdminSecret99!");
        props.getCredentials().setDoctorPassword("DoctorSecret99!");
        props.getCredentials().setPatientPassword("PatientSecret99!");

        assertThat(props.toString())
                .doesNotContain("AdminSecret99!")
                .doesNotContain("DoctorSecret99!")
                .doesNotContain("PatientSecret99!")
                .contains("REDACTED");
        assertThat(props.getCredentials().toString()).isEqualTo("Credentials{[REDACTED]}");
    }

    @Test
    @DisplayName("Standard dev run executes guard and seed service without reset")
    void standardRunSeedsWithoutReset() {
        DemoEnvironmentGuard guard = mock(DemoEnvironmentGuard.class);
        DemoDataSeedService seedService = mock(DemoDataSeedService.class);
        DemoDataResetService resetService = mock(DemoDataResetService.class);

        DemoDataProperties props = new DemoDataProperties();
        props.getSeed().setEnabled(true);
        props.getReset().setEnabled(false);

        DemoDataSeeder seeder = new DemoDataSeeder(guard, props, seedService, resetService);
        seeder.run();

        InOrder order = inOrder(guard, seedService);
        order.verify(guard).assertSafeForSeeding();
        order.verify(seedService).seedDemoData();
        verify(resetService, never()).resetDemoData();
    }

    @Test
    @DisplayName("Reset + reseed run invokes reset before reseed after guard checks")
    void resetAndReseedExecutesInStrictOrder() {
        DemoEnvironmentGuard guard = mock(DemoEnvironmentGuard.class);
        DemoDataSeedService seedService = mock(DemoDataSeedService.class);
        DemoDataResetService resetService = mock(DemoDataResetService.class);

        DemoDataProperties props = new DemoDataProperties();
        props.getSeed().setEnabled(true);
        props.getReset().setEnabled(true);
        props.getReset().setReseed(true);

        DemoDataSeeder seeder = new DemoDataSeeder(guard, props, seedService, resetService);
        seeder.run();

        InOrder order = inOrder(guard, resetService, seedService);
        order.verify(guard).assertSafeForReset();
        order.verify(resetService).resetDemoData();
        order.verify(guard).assertSafeForSeeding();
        order.verify(seedService).seedDemoData();
    }

    @Test
    @DisplayName("Pure reset with reseed=false clears demo records without reseeding")
    void pureResetSkipsReseedWhenDisabled() {
        DemoEnvironmentGuard guard = mock(DemoEnvironmentGuard.class);
        DemoDataSeedService seedService = mock(DemoDataSeedService.class);
        DemoDataResetService resetService = mock(DemoDataResetService.class);

        DemoDataProperties props = new DemoDataProperties();
        props.getSeed().setEnabled(true);
        props.getReset().setEnabled(true);
        props.getReset().setReseed(false);

        DemoDataSeeder seeder = new DemoDataSeeder(guard, props, seedService, resetService);
        seeder.run();

        verify(guard).assertSafeForReset();
        verify(resetService).resetDemoData();
        verify(seedService, never()).seedDemoData();
    }
}
