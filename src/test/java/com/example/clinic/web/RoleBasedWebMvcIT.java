package com.example.clinic.web;

import com.example.clinic.AbstractIntegrationTest;
import com.example.clinic.booking.domain.AppointmentStatus;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentJpaRepository;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaEntity;
import com.example.clinic.booking.infrastructure.persistence.AppointmentStatusHistoryJpaRepository;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.Role;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.BranchOpeningHours;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.StaffServiceEligibility;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * End-to-end integration tests over real PostgreSQL container verifying MVC flows,
 * transactional boundaries, authorization scoping, and database persistence (TC-027, TC-028, TC-045).
 */
@AutoConfigureMockMvc
@Transactional
class RoleBasedWebMvcIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BranchOpeningHoursRepository branchOpeningHoursRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Autowired
    private BranchServiceRepository branchServiceRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private StaffAssignmentRepository staffAssignmentRepository;

    @Autowired
    private StaffServiceEligibilityRepository staffServiceEligibilityRepository;

    @Autowired
    private WeeklyAvailabilityRepository weeklyAvailabilityRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private AppointmentStatusHistoryJpaRepository statusHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization organization;
    private Branch branch;
    private Department department;
    private ServiceDefinition serviceDefinition;
    private BranchService branchService;
    private UserAccount patientAccount;
    private PatientProfile patientProfile;
    private UserAccount doctorAccount;
    private StaffProfile doctorProfile;
    private StaffAssignment doctorAssignment;
    private UserAccount adminAccount;

    @BeforeEach
    void setUpTestData() {
        String encodedPw = passwordEncoder.encode("Password123!");

        // 1. Organization & Branch
        organization = organizationRepository.saveAndFlush(new Organization("IT Test Hospital"));
        branch = branchRepository.saveAndFlush(new Branch(organization, "IT Merkez Şube", "Europe/Istanbul", "İstanbul"));

        // All weekdays 08:00 - 18:00
        for (short d = 1; d <= 7; d++) {
            branchOpeningHoursRepository.saveAndFlush(new BranchOpeningHours(branch, d, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        }

        department = departmentRepository.saveAndFlush(new Department(branch, "Kardiyoloji"));

        // 2. Service
        serviceDefinition = serviceDefinitionRepository.saveAndFlush(new ServiceDefinition(organization, "Ekokardiyografi", "Kalp ultrasonu"));
        branchService = branchServiceRepository.saveAndFlush(new BranchService(branch, serviceDefinition, 30, Money.of(new BigDecimal("750.00"), Currency.getInstance("TRY"))));

        // 3. Doctor
        doctorAccount = userAccountRepository.saveAndFlush(new UserAccount("it_doctor@clinic.test", encodedPw));
        doctorProfile = staffProfileRepository.saveAndFlush(new StaffProfile(doctorAccount, "Dr. Ali Vural", "5551112233", StaffType.DOCTOR, "Kardiyoloji"));
        doctorAssignment = staffAssignmentRepository.saveAndFlush(new StaffAssignment(doctorProfile, branch, department));
        staffServiceEligibilityRepository.saveAndFlush(new StaffServiceEligibility(doctorAssignment, branchService));
        membershipRepository.saveAndFlush(new Membership(doctorAccount, organization, branch, Role.ROLE_STAFF));

        // Weekly availability: 09:00 - 17:00 every day
        for (short d = 1; d <= 7; d++) {
            weeklyAvailabilityRepository.saveAndFlush(new WeeklyAvailability(doctorAssignment, d, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        }

        // 4. Patient
        patientAccount = userAccountRepository.saveAndFlush(new UserAccount("it_patient@clinic.test", encodedPw));
        patientProfile = patientProfileRepository.saveAndFlush(new PatientProfile(patientAccount, "Zeynep Arslan", "5559998877"));
        membershipRepository.saveAndFlush(new Membership(patientAccount, organization, null, Role.ROLE_PATIENT));

        // 5. Admin
        adminAccount = userAccountRepository.saveAndFlush(new UserAccount("it_admin@clinic.test", encodedPw));
        membershipRepository.saveAndFlush(new Membership(adminAccount, organization, null, Role.ROLE_ADMIN));
    }

    @Test
    @DisplayName("End-to-end: Patient books appointment, doctor approves, patient verifies CONFIRMED status")
    void endToEndPatientBookingAndDoctorApproval() throws Exception {
        LocalDate appointmentDate = LocalDate.now().plusDays(2);
        LocalTime startTime = LocalTime.of(10, 0);

        // 1. Patient submits booking form
        mockMvc.perform(post("/patient/book")
                .with(csrf())
                .with(user("it_patient@clinic.test").roles("PATIENT"))
                .param("branchId", branch.getId().toString())
                .param("branchServiceId", branchService.getId().toString())
                .param("staffId", doctorProfile.getId().toString())
                .param("appointmentDate", appointmentDate.toString())
                .param("startTime", "10:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/appointments"))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify persisted appointment in PostgreSQL
        List<AppointmentJpaEntity> appointments = appointmentJpaRepository.findByPatientIdOrderByStartAtUtcDesc(patientProfile.getId());
        assertThat(appointments).hasSize(1);
        AppointmentJpaEntity appointment = appointments.get(0);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getServiceNameSnapshot()).isEqualTo("Ekokardiyografi");

        // 2. Doctor opens schedule and approves appointment
        mockMvc.perform(post("/staff/appointments/{id}/approve", appointment.getId())
                .with(csrf())
                .with(user("it_doctor@clinic.test").roles("STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/schedule"))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify status transitioned to CONFIRMED
        AppointmentJpaEntity updated = appointmentJpaRepository.findById(appointment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        // Verify status history audit records in DB
        var history = statusHistoryRepository.findByAppointmentIdOrderByOccurredAtAsc(appointment.getId());
        assertThat(history).hasSize(2); // PENDING initial + CONFIRMED transition
        assertThat(history.get(1).getToStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("End-to-end: Patient books appointment and cancels before start time")
    void endToEndPatientBookingAndCancellation() throws Exception {
        LocalDate appointmentDate = LocalDate.now().plusDays(3);

        mockMvc.perform(post("/patient/book")
                .with(csrf())
                .with(user("it_patient@clinic.test").roles("PATIENT"))
                .param("branchId", branch.getId().toString())
                .param("branchServiceId", branchService.getId().toString())
                .param("staffId", doctorProfile.getId().toString())
                .param("appointmentDate", appointmentDate.toString())
                .param("startTime", "11:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/appointments"));

        AppointmentJpaEntity appointment = appointmentJpaRepository.findByPatientIdOrderByStartAtUtcDesc(patientProfile.getId()).get(0);

        // Patient cancels
        mockMvc.perform(post("/patient/appointments/{id}/cancel", appointment.getId())
                .with(csrf())
                .with(user("it_patient@clinic.test").roles("PATIENT"))
                .param("reason", "Hasta tarafından randevu iptal edildi."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/appointments"))
                .andExpect(flash().attributeExists("successMessage"));

        AppointmentJpaEntity cancelled = appointmentJpaRepository.findById(appointment.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("End-to-end: Admin creates branch and department under authorized organization")
    void endToEndAdminBranchAndDepartmentCreation() throws Exception {
        mockMvc.perform(post("/admin/branches")
                .with(csrf())
                .with(user("it_admin@clinic.test").roles("ADMIN"))
                .param("organizationId", organization.getId().toString())
                .param("name", "Anadolu Şubesi")
                .param("timeZone", "Europe/Istanbul")
                .param("address", "Üsküdar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branches"));

        Branch createdBranch = branchRepository.findByOrganizationId(organization.getId()).stream()
                .filter(b -> "Anadolu Şubesi".equals(b.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(createdBranch.isActive()).isTrue();

        mockMvc.perform(post("/admin/departments")
                .with(csrf())
                .with(user("it_admin@clinic.test").roles("ADMIN"))
                .param("branchId", createdBranch.getId().toString())
                .param("name", "Nöroloji"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/departments?branchId=" + createdBranch.getId()));

        assertThat(departmentRepository.findByBranchIdAndName(createdBranch.getId(), "Nöroloji")).isPresent();
    }

    @Test
    @DisplayName("End-to-end: Rejected appointment writes audit history and immediately releases booking slot")
    void rejectionWritesHistoryAndReleasesSlot() throws Exception {
        LocalDate appointmentDate = LocalDate.now().plusDays(4);

        mockMvc.perform(post("/patient/book")
                        .with(csrf())
                        .with(user("it_patient@clinic.test").roles("PATIENT"))
                        .param("branchId", branch.getId().toString())
                        .param("branchServiceId", branchService.getId().toString())
                        .param("staffId", doctorProfile.getId().toString())
                        .param("appointmentDate", appointmentDate.toString())
                        .param("startTime", "14:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/appointments"));

        AppointmentJpaEntity booked = appointmentJpaRepository.findAll().stream()
                .filter(a -> a.getPatientId().equals(patientProfile.getId()) && a.getStatus() == AppointmentStatus.PENDING)
                .findFirst()
                .orElseThrow();

        // While PENDING, 14:00 is blocked in /patient/slots
        mockMvc.perform(get("/patient/slots")
                        .with(user("it_patient@clinic.test").roles("PATIENT"))
                        .param("staffId", doctorProfile.getId().toString())
                        .param("branchServiceId", branchService.getId().toString())
                        .param("date", appointmentDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasItem("14:00"))));

        // Doctor rejects with reason
        mockMvc.perform(post("/staff/appointments/{id}/reject", booked.getId())
                        .with(csrf())
                        .with(user(doctorAccount.getEmail()).roles("STAFF"))
                        .param("reason", "Acil konsültasyon sebebiyle ertelendi.")
                        .param("date", appointmentDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        AppointmentJpaEntity rejected = appointmentJpaRepository.findById(booked.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(AppointmentStatus.REJECTED);

        List<AppointmentStatusHistoryJpaEntity> history = statusHistoryRepository
                .findByAppointmentIdOrderByOccurredAtAscIdAsc(booked.getId());
        assertThat(history).extracting(AppointmentStatusHistoryJpaEntity::getToStatus)
                .containsExactly(AppointmentStatus.PENDING, AppointmentStatus.REJECTED);

        // Once REJECTED, 14:00 slot must immediately be available again
        mockMvc.perform(get("/patient/slots")
                        .with(user("it_patient@clinic.test").roles("PATIENT"))
                        .param("staffId", doctorProfile.getId().toString())
                        .param("branchServiceId", branchService.getId().toString())
                        .param("date", appointmentDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("14:00")));
    }

    @Test
    @DisplayName("End-to-end: Multi-tenant administrator isolation hides foreign staff and rejects cross-tenant IDs with 403")
    void crossTenantAdminIsolationEnforcedAcrossMvcRoutes() throws Exception {
        Organization foreignOrg = organizationRepository.saveAndFlush(new Organization("Yabancı Klinik A.Ş."));
        branchRepository.saveAndFlush(new Branch(foreignOrg, "Yabancı Şube", "Europe/Istanbul", "Ankara"));
        UserAccount foreignAdmin = userAccountRepository.saveAndFlush(
                new UserAccount("foreign_admin@clinic.test", passwordEncoder.encode("Secret123!"))
        );
        membershipRepository.saveAndFlush(new Membership(foreignAdmin, foreignOrg, null, Role.ROLE_ADMIN));

        // Scoped staff list must not leak Tenant A doctor to Tenant B admin
        mockMvc.perform(get("/admin/staff")
                        .with(user("foreign_admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(doctorProfile.getFullName()))));

        // Direct IDOR attempt on Tenant A branch departments or staff availability returns 403 Forbidden
        mockMvc.perform(get("/admin/departments")
                        .with(user("foreign_admin@clinic.test").roles("ADMIN"))
                        .param("branchId", branch.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));

        mockMvc.perform(get("/admin/staff/{id}/availability", doctorProfile.getId())
                        .with(user("foreign_admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    @Test
    @DisplayName("End-to-end: Double booking the same doctor slot redirects with safe Turkish conflict flash message")
    void duplicateSlotBookingShowsConflictMessageSafely() throws Exception {
        LocalDate appointmentDate = LocalDate.now().plusDays(5);

        mockMvc.perform(post("/patient/book")
                        .with(csrf())
                        .with(user("it_patient@clinic.test").roles("PATIENT"))
                        .param("branchId", branch.getId().toString())
                        .param("branchServiceId", branchService.getId().toString())
                        .param("staffId", doctorProfile.getId().toString())
                        .param("appointmentDate", appointmentDate.toString())
                        .param("startTime", "15:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/appointments"));

        mockMvc.perform(post("/patient/book")
                        .with(csrf())
                        .with(user("it_patient@clinic.test").roles("PATIENT"))
                        .param("branchId", branch.getId().toString())
                        .param("branchServiceId", branchService.getId().toString())
                        .param("staffId", doctorProfile.getId().toString())
                        .param("appointmentDate", appointmentDate.toString())
                        .param("startTime", "15:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/book"))
                .andExpect(model().attribute("errorMessage", containsString("çakışma")))
                .andExpect(content().string(containsString("çakışma")));
    }

    @Test
    @DisplayName("End-to-end: Role-aware navbar renders role links exclusively for authorized panels")
    void navbarRendersRoleAppropriateLinks() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("it_admin@clinic.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/branches")))
                .andExpect(content().string(containsString("/admin/hours")))
                .andExpect(content().string(containsString("/admin/departments")))
                .andExpect(content().string(containsString("/admin/services")))
                .andExpect(content().string(containsString("/admin/staff")));

        mockMvc.perform(get("/patient/appointments")
                        .with(user("it_patient@clinic.test").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/patient/book")))
                .andExpect(content().string(containsString("/patient/appointments")))
                .andExpect(content().string(not(containsString("/admin/branches"))));
    }
}
