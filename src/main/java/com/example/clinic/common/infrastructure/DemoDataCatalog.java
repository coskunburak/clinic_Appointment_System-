package com.example.clinic.common.infrastructure;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Deterministic identifiers and natural keys for the development-only demonstration dataset (FR-034, NFR-005).
 * Namespaced UUID v3 generation ensures idempotent seeding and scoped demo resets without mutating unrelated data.
 */
public final class DemoDataCatalog {

    private static final String NAMESPACE_PREFIX = "clinic-demo:";

    public static final String ORG_NAME = "Örnek Sağlık Grubu";
    public static final String BRANCH_ZONE_ISTANBUL = "Europe/Istanbul";
    public static final String CURRENCY_TRY = "TRY";

    public static final String BRANCH_MERKEZ_NAME = "Merkez Hastanesi";
    public static final String BRANCH_MERKEZ_ADDRESS = "Kadıköy, İstanbul";
    public static final String BRANCH_SAHIL_NAME = "Sahil Polikliniği";
    public static final String BRANCH_SAHIL_ADDRESS = "Beşiktaş, İstanbul";

    public static final String DEPT_KARDIYOLOJI = "Kardiyoloji";
    public static final String DEPT_RADYOLOJI = "Radyoloji";
    public static final String DEPT_DAHILIYE = "Dahiliye";
    public static final String DEPT_LABORATUVAR = "Laboratuvar";

    public static final String SERVICE_GENEL_MUAYENE = "Genel Muayene";
    public static final String SERVICE_EKO_CEKIMI = "Eko Çekimi";
    public static final String SERVICE_RADYOLOJI_AKCIGER = "Radyoloji Akciğer Grafisi";
    public static final String SERVICE_KAN_TAHLILI = "Kan Tahlili";

    public static final int DURATION_GENEL_MUAYENE = 30;
    public static final int DURATION_EKO_CEKIMI = 45;
    public static final int DURATION_RADYOLOJI_AKCIGER = 20;
    public static final int DURATION_KAN_TAHLILI = 15;

    public static final BigDecimal FEE_GENEL_MUAYENE = new BigDecimal("1500.00");
    public static final BigDecimal FEE_EKO_CEKIMI = new BigDecimal("2500.00");
    public static final BigDecimal FEE_RADYOLOJI_AKCIGER = new BigDecimal("1200.00");
    public static final BigDecimal FEE_KAN_TAHLILI = new BigDecimal("600.00");

    public static final String ADMIN_EMAIL = "admin@clinic.test";
    public static final String DOCTOR_AHMET_EMAIL = "doctor.ahmet@clinic.test";
    public static final String DOCTOR_AYSE_EMAIL = "doctor.ayse@clinic.test";
    public static final String PATIENT_ALI_EMAIL = "patient.ali@clinic.test";
    public static final String PATIENT_ZEYNEP_EMAIL = "patient.zeynep@clinic.test";

    public static final UUID ORG_ID = id("org:ornek-saglik-grubu");

    public static final UUID BRANCH_MERKEZ_ID = id("branch:merkez-hastanesi");
    public static final UUID BRANCH_SAHIL_ID = id("branch:sahil-poliklinigi");

    public static final UUID DEPT_MERKEZ_KARDIYOLOJI_ID = id("dept:merkez:kardiyoloji");
    public static final UUID DEPT_MERKEZ_RADYOLOJI_ID = id("dept:merkez:radyoloji");
    public static final UUID DEPT_MERKEZ_DAHILIYE_ID = id("dept:merkez:dahiliye");
    public static final UUID DEPT_SAHIL_DAHILIYE_ID = id("dept:sahil:dahiliye");
    public static final UUID DEPT_SAHIL_LABORATUVAR_ID = id("dept:sahil:laboratuvar");

    public static final UUID SERVICE_DEF_GENEL_MUAYENE_ID = id("service-def:genel-muayene");
    public static final UUID SERVICE_DEF_EKO_CEKIMI_ID = id("service-def:eko-cekimi");
    public static final UUID SERVICE_DEF_RADYOLOJI_AKCIGER_ID = id("service-def:radyoloji-akciger-grafisi");
    public static final UUID SERVICE_DEF_KAN_TAHLILI_ID = id("service-def:kan-tahlili");

    public static final UUID BRANCH_SERVICE_MERKEZ_GENEL_ID = id("branch-service:merkez:genel-muayene");
    public static final UUID BRANCH_SERVICE_MERKEZ_EKO_ID = id("branch-service:merkez:eko-cekimi");
    public static final UUID BRANCH_SERVICE_MERKEZ_RADYOLOJI_ID = id("branch-service:merkez:radyoloji-akciger");
    public static final UUID BRANCH_SERVICE_SAHIL_GENEL_ID = id("branch-service:sahil:genel-muayene");
    public static final UUID BRANCH_SERVICE_SAHIL_KAN_ID = id("branch-service:sahil:kan-tahlili");

    public static final UUID USER_ADMIN_ID = id("user:admin");
    public static final UUID USER_DOCTOR_AHMET_ID = id("user:doctor-ahmet");
    public static final UUID USER_DOCTOR_AYSE_ID = id("user:doctor-ayse");
    public static final UUID USER_PATIENT_ALI_ID = id("user:patient-ali");
    public static final UUID USER_PATIENT_ZEYNEP_ID = id("user:patient-zeynep");

    public static final UUID STAFF_AHMET_ID = id("staff:ahmet-kaya");
    public static final UUID STAFF_AYSE_ID = id("staff:ayse-demir");
    public static final UUID PATIENT_ALI_ID = id("patient:ali-yilmaz");
    public static final UUID PATIENT_ZEYNEP_ID = id("patient:zeynep-kaya");

    public static final UUID MEMBERSHIP_ADMIN_ID = id("membership:admin");
    public static final UUID MEMBERSHIP_AHMET_ID = id("membership:doctor-ahmet");
    public static final UUID MEMBERSHIP_AYSE_ID = id("membership:doctor-ayse");
    public static final UUID MEMBERSHIP_ALI_ID = id("membership:patient-ali");
    public static final UUID MEMBERSHIP_ZEYNEP_ID = id("membership:patient-zeynep");

    public static final UUID ASSIGNMENT_AHMET_MERKEZ_ID = id("assignment:ahmet:merkez-kardiyoloji");
    public static final UUID ASSIGNMENT_AYSE_SAHIL_ID = id("assignment:ayse:sahil-dahiliye");

    public static final UUID EXCEPTION_AHMET_PARTIAL_LEAVE_ID = id("exception:ahmet:partial-leave");
    public static final UUID EXCEPTION_AHMET_FULL_DAY_LEAVE_ID = id("exception:ahmet:full-day-leave");
    public static final UUID EXCEPTION_AYSE_OVERTIME_ID = id("exception:ayse:overtime");

    public static final UUID APPT_COMPLETED_AHMET_ID = id("appointment:past-completed-ahmet");
    public static final UUID APPT_COMPLETED_AYSE_ID = id("appointment:past-completed-ayse");
    public static final UUID APPT_CONFIRMED_AHMET_ID = id("appointment:future-confirmed-ahmet");
    public static final UUID APPT_PENDING_AHMET_ID = id("appointment:future-pending-ahmet");
    public static final UUID APPT_PENDING_AYSE_ID = id("appointment:future-pending-ayse");
    public static final UUID APPT_REJECTED_AYSE_ID = id("appointment:future-rejected-ayse");
    public static final UUID APPT_CANCELLED_AHMET_ID = id("appointment:future-cancelled-ahmet");

    public static final Set<UUID> ALL_BRANCH_IDS = Set.of(BRANCH_MERKEZ_ID, BRANCH_SAHIL_ID);

    public static final Set<UUID> ALL_DEPARTMENT_IDS = Set.of(
            DEPT_MERKEZ_KARDIYOLOJI_ID,
            DEPT_MERKEZ_RADYOLOJI_ID,
            DEPT_MERKEZ_DAHILIYE_ID,
            DEPT_SAHIL_DAHILIYE_ID,
            DEPT_SAHIL_LABORATUVAR_ID
    );

    public static final Set<UUID> ALL_SERVICE_DEFINITION_IDS = Set.of(
            SERVICE_DEF_GENEL_MUAYENE_ID,
            SERVICE_DEF_EKO_CEKIMI_ID,
            SERVICE_DEF_RADYOLOJI_AKCIGER_ID,
            SERVICE_DEF_KAN_TAHLILI_ID
    );

    public static final Set<UUID> ALL_BRANCH_SERVICE_IDS = Set.of(
            BRANCH_SERVICE_MERKEZ_GENEL_ID,
            BRANCH_SERVICE_MERKEZ_EKO_ID,
            BRANCH_SERVICE_MERKEZ_RADYOLOJI_ID,
            BRANCH_SERVICE_SAHIL_GENEL_ID,
            BRANCH_SERVICE_SAHIL_KAN_ID
    );

    public static final List<String> ALL_ACCOUNT_EMAILS = List.of(
            ADMIN_EMAIL,
            DOCTOR_AHMET_EMAIL,
            DOCTOR_AYSE_EMAIL,
            PATIENT_ALI_EMAIL,
            PATIENT_ZEYNEP_EMAIL
    );

    public static final Set<UUID> ALL_USER_IDS = Set.of(
            USER_ADMIN_ID,
            USER_DOCTOR_AHMET_ID,
            USER_DOCTOR_AYSE_ID,
            USER_PATIENT_ALI_ID,
            USER_PATIENT_ZEYNEP_ID
    );

    public static final Set<UUID> ALL_STAFF_PROFILE_IDS = Set.of(STAFF_AHMET_ID, STAFF_AYSE_ID);
    public static final Set<UUID> ALL_PATIENT_PROFILE_IDS = Set.of(PATIENT_ALI_ID, PATIENT_ZEYNEP_ID);
    public static final Set<UUID> ALL_ASSIGNMENT_IDS = Set.of(ASSIGNMENT_AHMET_MERKEZ_ID, ASSIGNMENT_AYSE_SAHIL_ID);

    public static final Set<UUID> ALL_APPOINTMENT_IDS = Set.of(
            APPT_COMPLETED_AHMET_ID,
            APPT_COMPLETED_AYSE_ID,
            APPT_CONFIRMED_AHMET_ID,
            APPT_PENDING_AHMET_ID,
            APPT_PENDING_AYSE_ID,
            APPT_REJECTED_AYSE_ID,
            APPT_CANCELLED_AHMET_ID
    );

    private DemoDataCatalog() {
    }

    public static UUID id(String key) {
        return UUID.nameUUIDFromBytes((NAMESPACE_PREFIX + key).getBytes(StandardCharsets.UTF_8));
    }
}
