package com.example.clinic.web;

import com.example.clinic.common.infrastructure.DemoDataResetService;
import com.example.clinic.common.infrastructure.DemoDataSeedService;
import com.example.clinic.common.infrastructure.DemoDataSeeder;
import com.example.clinic.identity.web.LoginController;
import com.example.clinic.web.admin.AdminBranchController;
import com.example.clinic.web.admin.AdminDashboardController;
import com.example.clinic.web.admin.AdminDepartmentController;
import com.example.clinic.web.admin.AdminServiceController;
import com.example.clinic.web.admin.AdminStaffController;
import com.example.clinic.web.error.CustomErrorController;
import com.example.clinic.web.patient.PatientBookingController;
import com.example.clinic.web.staff.StaffScheduleController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 * dev-only demo seed/reset services, or field-injected dependencies.
 */
class WebArchitectureTest {

    private final List<Class<?>> controllerClasses = List.of(
            LoginController.class,
            PatientBookingController.class,
            StaffScheduleController.class,
            AdminDashboardController.class,
            AdminBranchController.class,
            AdminDepartmentController.class,
            AdminServiceController.class,
            AdminStaffController.class,
            CustomErrorController.class
    );

    private final List<Class<?>> forbiddenDemoInfrastructure = List.of(
            DemoDataSeeder.class,
            DemoDataSeedService.class,
            DemoDataResetService.class
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

    @Test
    @DisplayName("NFR-010 / FR-034: Controllers never expose or inject demo seed/reset services and avoid field injection")
    void controllersNeverDependOnDemoInfrastructureOrFieldInjection() {
        for (Class<?> clazz : controllerClasses) {
            for (Field field : clazz.getDeclaredFields()) {
                assertThat(field.isAnnotationPresent(Autowired.class))
                        .as("Controller %s should use constructor injection rather than @Autowired field %s",
                                clazz.getSimpleName(), field.getName())
                        .isFalse();
                for (Class<?> forbidden : forbiddenDemoInfrastructure) {
                    assertThat(forbidden.isAssignableFrom(field.getType()))
                            .as("Controller %s must not inject %s", clazz.getSimpleName(), forbidden.getSimpleName())
                            .isFalse();
                }
            }

            for (Constructor<?> constructor : clazz.getConstructors()) {
                for (Class<?> paramType : constructor.getParameterTypes()) {
                    for (Class<?> forbidden : forbiddenDemoInfrastructure) {
                        assertThat(forbidden.isAssignableFrom(paramType))
                                .as("Controller %s must not accept %s in constructor",
                                        clazz.getSimpleName(), forbidden.getSimpleName())
                                .isFalse();
                    }
                }
            }
        }
    }
}
