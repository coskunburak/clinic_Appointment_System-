package com.example.clinic.common.infrastructure;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Development-only startup hook executing deterministic demo data seeding and, when explicitly opted in,
 * scoped demo reset and reseed (FR-034, NFR-005).
 */
@Component
@Profile("dev")
@ConditionalOnExpression("${clinic.demo.seed.enabled:false} || ${clinic.demo.reset.enabled:false}")
public class DemoDataSeeder implements CommandLineRunner {

    private final DemoEnvironmentGuard guard;
    private final DemoDataProperties properties;
    private final DemoDataSeedService seedService;
    private final DemoDataResetService resetService;

    public DemoDataSeeder(DemoEnvironmentGuard guard,
                          DemoDataProperties properties,
                          DemoDataSeedService seedService,
                          DemoDataResetService resetService) {
        this.guard = Objects.requireNonNull(guard, "guard must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.seedService = Objects.requireNonNull(seedService, "seedService must not be null");
        this.resetService = Objects.requireNonNull(resetService, "resetService must not be null");
    }

    @Override
    public void run(String... args) {
        if (properties.getReset().isEnabled()) {
            guard.assertSafeForReset();
            resetService.resetDemoData();
            if (properties.getReset().isReseed() && properties.getSeed().isEnabled()) {
                guard.assertSafeForSeeding();
                seedService.seedDemoData();
            }
            return;
        }

        if (properties.getSeed().isEnabled()) {
            guard.assertSafeForSeeding();
            seedService.seedDemoData();
        }
    }
}
