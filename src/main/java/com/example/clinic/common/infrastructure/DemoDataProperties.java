package com.example.clinic.common.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties governing development-only demo seeding and controlled reset behavior.
 * Disabled by default outside explicit {@code dev} configuration.
 */
@ConfigurationProperties(prefix = "clinic.demo")
public class DemoDataProperties {

    private Seed seed = new Seed();
    private Reset reset = new Reset();
    private Credentials credentials = new Credentials();

    public Seed getSeed() {
        return seed;
    }

    public void setSeed(Seed seed) {
        this.seed = seed != null ? seed : new Seed();
    }

    public Reset getReset() {
        return reset;
    }

    public void setReset(Reset reset) {
        this.reset = reset != null ? reset : new Reset();
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials != null ? credentials : new Credentials();
    }

    @Override
    public String toString() {
        return "DemoDataProperties{seedEnabled=" + seed.isEnabled()
                + ", resetEnabled=" + reset.isEnabled()
                + ", resetReseed=" + reset.isReseed()
                + ", credentials=[REDACTED]}";
    }

    public static class Seed {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Reset {
        private boolean enabled = false;
        private boolean reseed = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isReseed() {
            return reseed;
        }

        public void setReseed(boolean reseed) {
            this.reseed = reseed;
        }
    }

    public static class Credentials {
        private String adminPassword;
        private String doctorPassword;
        private String patientPassword;

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }

        public String getDoctorPassword() {
            return doctorPassword;
        }

        public void setDoctorPassword(String doctorPassword) {
            this.doctorPassword = doctorPassword;
        }

        public String getPatientPassword() {
            return patientPassword;
        }

        public void setPatientPassword(String patientPassword) {
            this.patientPassword = patientPassword;
        }

        @Override
        public String toString() {
            return "Credentials{[REDACTED]}";
        }
    }
}
