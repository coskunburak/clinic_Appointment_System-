package com.example.clinic.scheduling.application;

import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.common.domain.TimeInterval;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.BranchOpeningHours;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.scheduling.application.port.AppointmentSchedulePort;
import com.example.clinic.scheduling.domain.TimeSlot;
import com.example.clinic.staff.domain.AvailabilityException;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.WeeklyAvailability;
import com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository;
import com.example.clinic.staff.infrastructure.StaffAssignmentRepository;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
import com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityCalculatorTest {

    @Mock
    private StaffProfileRepository staffProfileRepository;
    @Mock
    private BranchServiceRepository branchServiceRepository;
    @Mock
    private StaffAssignmentRepository staffAssignmentRepository;
    @Mock
    private StaffServiceEligibilityRepository staffServiceEligibilityRepository;
    @Mock
    private BranchOpeningHoursRepository branchOpeningHoursRepository;
    @Mock
    private WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    @Mock
    private AvailabilityExceptionRepository availabilityExceptionRepository;
    @Mock
    private AppointmentSchedulePort appointmentSchedulePort;

    private Clock fixedClock;
    private AvailabilityCalculator calculator;

    private Organization org;
    private Branch branch;
    private Department dept;
    private StaffProfile staff;
    private StaffAssignment assignment;
    private ServiceDefinition serviceDef;
    private BranchService branchService;

    // A fixed Monday date: 2026-11-09
    private final LocalDate targetMonday = LocalDate.of(2026, 11, 9);
    private final ZoneId zoneId = ZoneId.of("Europe/Istanbul");

    @BeforeEach
    void setUp() {
        // Clock fixed to 2026-11-08 00:00:00 UTC (day before target Monday)
        fixedClock = Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneId.of("UTC"));

        calculator = new AvailabilityCalculator(
                staffProfileRepository,
                branchServiceRepository,
                staffAssignmentRepository,
                staffServiceEligibilityRepository,
                branchOpeningHoursRepository,
                weeklyAvailabilityRepository,
                availabilityExceptionRepository,
                appointmentSchedulePort,
                fixedClock
        );

        org = new Organization(UUID.randomUUID(), "Memorial", true, Instant.now());
        branch = new Branch(UUID.randomUUID(), org, "Kadikoy", "Europe/Istanbul", "Kadikoy Cad.", true, Instant.now());
        dept = new Department(UUID.randomUUID(), branch, "Kardiyoloji", true, Instant.now());
        UserAccount staffUser = new UserAccount(UUID.randomUUID(), "doc@clinic.test", "hash123", true, Instant.now());
        staff = new StaffProfile(staffUser, "Dr. Ahmet Kaya", "+905551112233", "DOCTOR", "Kardiyoloji");
        assignment = new StaffAssignment(staff, branch, dept);

        serviceDef = new ServiceDefinition(UUID.randomUUID(), org, "Genel Muayene", "Rutin Muayene", true, Instant.now());
        branchService = new BranchService(
                UUID.randomUUID(), org, branch, serviceDef, 30, Money.of(new BigDecimal("800.00"), "TRY"), true, Instant.now()
        );
    }

    private void mockStandardSetup() {
        when(branchServiceRepository.findById(branchService.getId())).thenReturn(Optional.of(branchService));
        when(staffProfileRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(staffAssignmentRepository.findByStaffIdAndBranchIdAndActiveTrue(staff.getId(), branch.getId()))
                .thenReturn(Optional.of(assignment));
        when(staffServiceEligibilityRepository.existsByAssignmentIdAndBranchServiceId(assignment.getId(), branchService.getId()))
                .thenReturn(true);

        // Branch open Monday 08:00 - 18:00
        BranchOpeningHours bHours = new BranchOpeningHours(branch, (short) 1, LocalTime.of(8, 0), LocalTime.of(18, 0));
        when(branchOpeningHoursRepository.findByBranchIdAndWeekday(branch.getId(), (short) 1))
                .thenReturn(List.of(bHours));

        // Staff available Monday 09:00 - 12:00
        WeeklyAvailability wAvail = new WeeklyAvailability(assignment, (short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0));
        when(weeklyAvailabilityRepository.findByAssignmentIdAndWeekday(assignment.getId(), (short) 1))
                .thenReturn(List.of(wAvail));
    }

    @Test
    @DisplayName("shouldCalculateAvailableSlotsCorrectly(): 09:00-12:00 mesaisi olan hekime 30 dk hizmet için slot üretme")
    void shouldCalculateAvailableSlotsCorrectly() {
        mockStandardSetup();
        when(availabilityExceptionRepository.findByAssignmentIdAndLocalDate(assignment.getId(), targetMonday))
                .thenReturn(Collections.emptyList());
        when(appointmentSchedulePort.findOccupiedIntervals(eq(staff.getId()), eq(targetMonday), any(ZoneId.class)))
                .thenReturn(Collections.emptyList());

        List<TimeSlot> slots = calculator.calculateAvailableSlots(staff.getId(), branchService.getId(), targetMonday);

        assertThat(slots).hasSize(6);
        assertThat(slots.stream().map(TimeSlot::startTime).toList()).containsExactly(
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30)
        );
        assertThat(slots.stream().map(TimeSlot::endTime).toList()).containsExactly(
                LocalTime.of(9, 30),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                LocalTime.of(12, 0)
        );
    }

    @Test
    @DisplayName("shouldExcludeFullDayLeaveFromSlots(): Hekimin tam gün izinli olduğu tarih boş liste ([]) döner")
    void shouldExcludeFullDayLeaveFromSlots() {
        mockStandardSetup();
        AvailabilityException leave = AvailabilityException.createFullDayLeave(assignment, targetMonday, "Yıllık İzin");
        when(availabilityExceptionRepository.findByAssignmentIdAndLocalDate(assignment.getId(), targetMonday))
                .thenReturn(List.of(leave));

        List<TimeSlot> slots = calculator.calculateAvailableSlots(staff.getId(), branchService.getId(), targetMonday);

        assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("shouldExcludeExistingAppointmentsFromSlots(): 10:00-10:30 arası dolu olan hekim için 10:00 slotu çıkarılır")
    void shouldExcludeExistingAppointmentsFromSlots() {
        mockStandardSetup();
        when(availabilityExceptionRepository.findByAssignmentIdAndLocalDate(assignment.getId(), targetMonday))
                .thenReturn(Collections.emptyList());

        // 10:00 - 10:30 appointment in Europe/Istanbul time
        Instant startUtc = targetMonday.atTime(10, 0).atZone(zoneId).toInstant();
        Instant endUtc = targetMonday.atTime(10, 30).atZone(zoneId).toInstant();
        TimeInterval appointmentInterval = TimeInterval.of(startUtc, endUtc);

        when(appointmentSchedulePort.findOccupiedIntervals(eq(staff.getId()), eq(targetMonday), any(ZoneId.class)))
                .thenReturn(List.of(appointmentInterval));

        List<TimeSlot> slots = calculator.calculateAvailableSlots(staff.getId(), branchService.getId(), targetMonday);

        assertThat(slots).hasSize(5);
        assertThat(slots.stream().map(TimeSlot::startTime).toList()).containsExactly(
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30)
        );
        assertThat(slots.stream().map(TimeSlot::startTime).toList()).doesNotContain(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("shouldExcludePartialLeaveFromSlots(): 10:00-11:00 kısmi izin slotları dışlar")
    void shouldExcludePartialLeaveFromSlots() {
        mockStandardSetup();
        AvailabilityException partialLeave = AvailabilityException.createPartialLeave(
                assignment, targetMonday, LocalTime.of(10, 0), LocalTime.of(11, 0), "Doktor Kongre Sunumu"
        );
        when(availabilityExceptionRepository.findByAssignmentIdAndLocalDate(assignment.getId(), targetMonday))
                .thenReturn(List.of(partialLeave));
        when(appointmentSchedulePort.findOccupiedIntervals(eq(staff.getId()), eq(targetMonday), any(ZoneId.class)))
                .thenReturn(Collections.emptyList());

        List<TimeSlot> slots = calculator.calculateAvailableSlots(staff.getId(), branchService.getId(), targetMonday);

        assertThat(slots).hasSize(4);
        assertThat(slots.stream().map(TimeSlot::startTime).toList()).containsExactly(
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30)
        );
    }

    @Test
    @DisplayName("shouldReturnEmptyWhenStaffNotEligibleForService(): Yetkisiz hizmet için boş liste döner")
    void shouldReturnEmptyWhenStaffNotEligibleForService() {
        when(branchServiceRepository.findById(branchService.getId())).thenReturn(Optional.of(branchService));
        when(staffProfileRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(staffAssignmentRepository.findByStaffIdAndBranchIdAndActiveTrue(staff.getId(), branch.getId()))
                .thenReturn(Optional.of(assignment));
        when(staffServiceEligibilityRepository.existsByAssignmentIdAndBranchServiceId(assignment.getId(), branchService.getId()))
                .thenReturn(false);

        List<TimeSlot> slots = calculator.calculateAvailableSlots(staff.getId(), branchService.getId(), targetMonday);

        assertThat(slots).isEmpty();
        assertThat(calculator.isStaffEligible(staff.getId(), branchService.getId())).isFalse();
    }

    @Test
    @DisplayName("shouldReturnEmptyWhenTargetDateIsInThePast(): Geçmiş tarihler için boş liste döner")
    void shouldReturnEmptyWhenTargetDateIsInThePast() {
        when(branchServiceRepository.findById(branchService.getId())).thenReturn(Optional.of(branchService));

        LocalDate pastDate = LocalDate.of(2026, 11, 5);
        List<TimeSlot> slots = calculator.calculateAvailableSlots(staff.getId(), branchService.getId(), pastDate);

        assertThat(slots).isEmpty();
    }
}
