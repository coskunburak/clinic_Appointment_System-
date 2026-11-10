package com.example.clinic.booking;

import com.example.clinic.booking.application.AppointmentApprovalService;
import com.example.clinic.booking.domain.Appointment;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.domain.ServiceSnapshot;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaEntity;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.domain.TimeInterval;
import jakarta.persistence.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architectural compliance and invariant verification tests fulfilling Section 8.3.
 */
class AppointmentArchitectureTest {

    private static final List<String> FORBIDDEN_FRAMEWORK_PACKAGES = List.of(
            "org.springframework",
            "jakarta.persistence",
            "org.hibernate",
            "java.sql",
            "javax.sql"
    );

    @Test
    @DisplayName("Architecture: Appointment aggregate root is pure Java with zero framework imports")
    void appointmentMustBePureJavaPOJO() throws Exception {
        Path path = Paths.get("src/main/java/com/example/clinic/booking/domain/Appointment.java").toAbsolutePath();
        assertThat(path).exists();

        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                for (String forbidden : FORBIDDEN_FRAMEWORK_PACKAGES) {
                    assertThat(trimmed)
                            .withFailMessage("Appointment contains forbidden import: %s", trimmed)
                            .doesNotContain("import " + forbidden);
                }
            }
        }
    }

    @Test
    @DisplayName("Architecture: Appointment aggregate root has no unsafe public status setters")
    void appointmentMustHaveNoPublicStatusSetter() {
        Method[] methods = Appointment.class.getMethods();
        for (Method method : methods) {
            assertThat(method.getName().toLowerCase())
                    .withFailMessage("Unsafe public setter detected on Appointment domain aggregate: %s", method.getName())
                    .isNotEqualTo("setstatus");
        }
    }

    @Test
    @DisplayName("Architecture: Application layer does not depend directly on HTTP or web controllers")
    void applicationLayerMustNotDependOnWebControllers() throws Exception {
        Path path = Paths.get("src/main/java/com/example/clinic/booking/application/AppointmentApprovalService.java").toAbsolutePath();
        assertThat(path).exists();

        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                assertThat(trimmed)
                        .withFailMessage("AppointmentApprovalService must not import web/controller classes: %s", trimmed)
                        .doesNotContain(".web")
                        .doesNotContain("Controller")
                        .doesNotContain("jakarta.servlet")
                        .doesNotContain("org.springframework.web.bind");
            }
        }
    }

    @Test
    @DisplayName("Architecture: AppointmentStatusHistoryJpaEntity fields are updatable=false (append-only)")
    void statusHistoryMustBeAppendOnly() {
        Field[] fields = AppointmentStatusHistoryJpaEntity.class.getDeclaredFields();
        for (Field field : fields) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null && !"id".equals(field.getName())) {
                assertThat(columnAnnotation.updatable())
                        .withFailMessage("Field %s in AppointmentStatusHistoryJpaEntity must have updatable = false", field.getName())
                        .isFalse();
            }
        }

        // Verify no public setters exist on history entity
        Method[] methods = AppointmentStatusHistoryJpaEntity.class.getMethods();
        for (Method method : methods) {
            assertThat(method.getName().startsWith("set"))
                    .withFailMessage("AppointmentStatusHistoryJpaEntity must not expose public setters: %s", method.getName())
                    .isFalse();
        }
    }

    @Test
    @DisplayName("Architecture: AppointmentJpaEntity preserves all domain fields across round-trip conversion")
    void jpaEntityRoundTripPreservesAllFields() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID branchServiceId = UUID.randomUUID();
        Instant start = Instant.parse("2026-11-10T14:00:00Z");
        TimeInterval interval = TimeInterval.of(start, Duration.ofMinutes(45));
        ServiceSnapshot snapshot = new ServiceSnapshot(
                branchServiceId,
                "Ultrason Görüntüleme",
                45,
                Money.of(new BigDecimal("750.00"), Currency.getInstance("TRY"))
        );
        Instant createdAt = Instant.parse("2026-11-10T08:00:00Z");

        Appointment original = Appointment.reconstitute(
                id, patientId, staffId, branchId, branchServiceId,
                interval, snapshot, AppointmentStatus.CONFIRMED, createdAt, 3L
        );

        AppointmentJpaEntity entity = AppointmentJpaEntity.fromDomain(original, 3L);
        Appointment reconstituted = entity.toDomain();

        assertThat(reconstituted.getId()).isEqualTo(original.getId());
        assertThat(reconstituted.getPatientId()).isEqualTo(original.getPatientId());
        assertThat(reconstituted.getStaffId()).isEqualTo(original.getStaffId());
        assertThat(reconstituted.getBranchId()).isEqualTo(original.getBranchId());
        assertThat(reconstituted.getBranchServiceId()).isEqualTo(original.getBranchServiceId());
        assertThat(reconstituted.getInterval()).isEqualTo(original.getInterval());
        assertThat(reconstituted.getStatus()).isEqualTo(original.getStatus());
        assertThat(reconstituted.getServiceSnapshot().serviceName()).isEqualTo(original.getServiceSnapshot().serviceName());
        assertThat(reconstituted.getServiceSnapshot().durationMinutes()).isEqualTo(original.getServiceSnapshot().durationMinutes());
        assertThat(reconstituted.getServiceSnapshot().fee()).isEqualTo(original.getServiceSnapshot().fee());
        assertThat(reconstituted.getVersion()).isEqualTo(3L);
    }
}
