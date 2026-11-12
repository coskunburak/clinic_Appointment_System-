package com.example.clinic.web;

import com.example.clinic.web.admin.AdminManagementController;
import com.example.clinic.web.patient.PatientBookingController;
import com.example.clinic.web.staff.StaffScheduleController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architectural tests verifying Spring MVC layer boundaries (Section 3.1, NFR-010).
 * Enforces that controllers do not directly inject Spring Data JPA repositories,
 * preserving strict separation of concerns through application services.
 */
class WebArchitectureTest {

    private final List<Class<?>> controllerClasses = List.of(
            PatientBookingController.class,
            StaffScheduleController.class,
            AdminManagementController.class
    );

    @Test
    @DisplayName("NFR-010: Controllers are annotated with @Controller")
    void controllersAreAnnotatedWithController() {
        for (Class<?> clazz : controllerClasses) {
            assertThat(clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(RestController.class))
                    .as("Controller %s must be annotated with @Controller or @RestController", clazz.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("NFR-010: Controllers do not declare direct Spring Data JPA Repository dependencies")
    void controllersDoNotDependDirectlyOnRepositories() {
        for (Class<?> clazz : controllerClasses) {
            for (Field field : clazz.getDeclaredFields()) {
                assertThat(JpaRepository.class.isAssignableFrom(field.getType()))
                        .as("Controller %s has prohibited direct repository dependency: %s",
                                clazz.getSimpleName(), field.getName())
                        .isFalse();
            }

            for (Constructor<?> constructor : clazz.getConstructors()) {
                for (Class<?> paramType : constructor.getParameterTypes()) {
                    assertThat(JpaRepository.class.isAssignableFrom(paramType))
                            .as("Controller %s constructor accepts prohibited direct repository parameter: %s",
                                    clazz.getSimpleName(), paramType.getSimpleName())
                            .isFalse();
                }
            }
        }
    }
}
