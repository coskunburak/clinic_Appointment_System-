package com.example.clinic.booking.application;

import com.example.clinic.booking.application.dto.BranchSummaryDto;
import com.example.clinic.booking.application.dto.ServiceSummaryDto;
import com.example.clinic.booking.application.dto.StaffSummaryDto;
import com.example.clinic.catalog.domain.BranchService;
import com.example.clinic.catalog.domain.ServiceDefinition;
import com.example.clinic.catalog.infrastructure.BranchServiceRepository;
import com.example.clinic.common.domain.Money;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.StaffType;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.scheduling.application.AvailabilityCalculator;
import com.example.clinic.scheduling.domain.TimeSlot;
import com.example.clinic.staff.domain.StaffAssignment;
import com.example.clinic.staff.domain.StaffServiceEligibility;
import com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository;
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
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Isolated unit tests for PatientBookingQueryService.
 */
@ExtendWith(MockitoExtension.class)
class PatientBookingQueryServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchServiceRepository branchServiceRepository;

    @Mock
    private StaffServiceEligibilityRepository staffServiceEligibilityRepository;

    @Mock
    private AvailabilityCalculator availabilityCalculator;

    private PatientBookingQueryService service;
    private Organization organization;
    private Branch branch;

    @BeforeEach
    void setUp() {
        service = new PatientBookingQueryService(
                branchRepository,
                branchServiceRepository,
                staffServiceEligibilityRepository,
                availabilityCalculator
        );

        organization = new Organization("Test Org");
        branch = new Branch(organization, "Kadıköy Şubesi", "Europe/Istanbul", "İstanbul");
    }

    @Test
    @DisplayName("getActiveBranches filters by active organization and active branch")
    void getActiveBranchesFiltersActive() {
        when(branchRepository.findByActiveTrue()).thenReturn(List.of(branch));

        List<BranchSummaryDto> result = service.getActiveBranches();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Kadıköy Şubesi");
    }

    @Test
    @DisplayName("getActiveServicesForBranch returns active branch services")
    void getActiveServicesForBranch() {
        ServiceDefinition def = new ServiceDefinition(organization, "Göz Muayenesi", "Açıklama");
        BranchService bs = new BranchService(branch, def, 20, Money.of(BigDecimal.valueOf(400), Currency.getInstance("TRY")));

        when(branchServiceRepository.findByBranchIdAndActiveTrue(branch.getId()))
                .thenReturn(List.of(bs));

        List<ServiceSummaryDto> result = service.getActiveServicesForBranch(branch.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Göz Muayenesi");
        assertThat(result.get(0).durationMinutes()).isEqualTo(20);
        assertThat(result.get(0).feeAmount()).isEqualByComparingTo(BigDecimal.valueOf(400));
    }

    @Test
    @DisplayName("getEligibleStaffForService returns active doctors for the service")
    void getEligibleStaffForService() {
        UUID branchServiceId = UUID.randomUUID();
        UserAccount user = new UserAccount("doc@clinic.test", "hash");
        StaffProfile staff = new StaffProfile(user, "Dr. Mehmet Kaya", "+905551234567", StaffType.DOCTOR, "Göz");
        Department dept = new Department(branch, "Göz");
        StaffAssignment asg = new StaffAssignment(staff, branch, dept);

        ServiceDefinition def = new ServiceDefinition(organization, "Göz", "Açıklama");
        BranchService bs = new BranchService(branch, def, 20, Money.of(BigDecimal.valueOf(400), Currency.getInstance("TRY")));
        StaffServiceEligibility eligibility = new StaffServiceEligibility(asg, bs);

        when(staffServiceEligibilityRepository.findByBranchServiceId(branchServiceId))
                .thenReturn(List.of(eligibility));

        List<StaffSummaryDto> result = service.getEligibleStaffForService(branchServiceId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("Dr. Mehmet Kaya");
        assertThat(result.get(0).specialty()).isEqualTo("Göz");
    }

    @Test
    @DisplayName("getAvailableSlotTimes delegates to AvailabilityCalculator")
    void getAvailableSlotTimes() {
        UUID staffId = UUID.randomUUID();
        UUID branchServiceId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        TimeSlot slot1 = new TimeSlot(LocalTime.of(9, 0), LocalTime.of(9, 30));
        TimeSlot slot2 = new TimeSlot(LocalTime.of(9, 30), LocalTime.of(10, 0));

        when(availabilityCalculator.calculateAvailableSlots(staffId, branchServiceId, date))
                .thenReturn(List.of(slot1, slot2));

        List<LocalTime> result = service.getAvailableSlotTimes(staffId, branchServiceId, date);

        assertThat(result).containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30));
    }

    @Test
    @DisplayName("Filters out inactive service definitions, organizations, and departments from booking options")
    void filtersOutInactiveHierarchyFromBookingOptions() {
        ServiceDefinition activeDef = new ServiceDefinition(organization, "Aktif Hizmet", "Açıklama");
        ServiceDefinition inactiveDef = new ServiceDefinition(organization, "Pasif Hizmet", "Açıklama");
        inactiveDef.deactivate();

        BranchService activeBs = new BranchService(branch, activeDef, 30, Money.of(new BigDecimal("300.00"), Currency.getInstance("TRY")));
        BranchService inactiveDefBs = new BranchService(branch, inactiveDef, 30, Money.of(new BigDecimal("300.00"), Currency.getInstance("TRY")));

        when(branchServiceRepository.findByBranchIdAndActiveTrue(branch.getId()))
                .thenReturn(List.of(activeBs, inactiveDefBs));

        List<ServiceSummaryDto> services = service.getActiveServicesForBranch(branch.getId());
        assertThat(services).hasSize(1);
        assertThat(services.get(0).name()).isEqualTo("Aktif Hizmet");

        Department inactiveDept = new Department(branch, "Pasif Bölüm");
        inactiveDept.setActive(false);
        UserAccount docUser = new UserAccount("doc2@clinic.test", "hash");
        StaffProfile docProfile = new StaffProfile(docUser, "Dr. Pasif Bölüm", "+905551234568", StaffType.DOCTOR, "Cildiye");
        StaffAssignment inactiveDeptAssignment = new StaffAssignment(docProfile, branch, inactiveDept);
        StaffServiceEligibility ineligibleByDept = new StaffServiceEligibility(inactiveDeptAssignment, activeBs);

        when(staffServiceEligibilityRepository.findByBranchServiceId(activeBs.getId()))
                .thenReturn(List.of(ineligibleByDept));

        List<StaffSummaryDto> eligibleStaff = service.getEligibleStaffForService(activeBs.getId());
        assertThat(eligibleStaff).isEmpty();
    }

    @Test
    @DisplayName("Computes default booking date in branch local timezone across UTC midnight")
    void computesDefaultBookingDateInBranchTimeZone() {
        Clock utcLateNight = Clock.fixed(Instant.parse("2026-11-20T22:00:00Z"), ZoneId.of("UTC"));
        PatientBookingQueryService tzAware = new PatientBookingQueryService(
                branchRepository,
                branchServiceRepository,
                staffServiceEligibilityRepository,
                availabilityCalculator,
                utcLateNight
        );
        when(branchRepository.findById(branch.getId())).thenReturn(Optional.of(branch));

        // 22:00 UTC on Nov 20 == 01:00 on Nov 21 in Europe/Istanbul -> branch local date is Nov 21
        assertThat(tzAware.getDefaultBookingDate(branch.getId())).isEqualTo(LocalDate.of(2026, 11, 21));
    }
}
