package com.example.clinic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicAppointmentApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Context loads successfully with real PostgreSQL and validated JPA schema")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }
}
