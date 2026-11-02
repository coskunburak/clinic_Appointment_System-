package com.example.clinic;

import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.BranchOpeningHours;
import com.example.clinic.organization.domain.Department;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.DepartmentRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OrganizationRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private BranchOpeningHoursRepository branchOpeningHoursRepository;

    @Test
    @DisplayName("Should persist and retrieve organization by name and id")
    void shouldPersistAndRetrieveOrganization() {
        Organization org = new Organization("Şehir Sağlık Grubu");
        Organization saved = organizationRepository.saveAndFlush(org);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Şehir Sağlık Grubu");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<Organization> found = organizationRepository.findByName("Şehir Sağlık Grubu");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("Should persist branch under organization and retrieve by organizationId")
    void shouldPersistBranchUnderOrganization() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Anadolu Sağlık"));
        Branch branch = new Branch(org, "Kadıköy Polikliniği", "Europe/Istanbul", "Bağdat Cad. No: 123");
        Branch savedBranch = branchRepository.saveAndFlush(branch);

        assertThat(savedBranch.getId()).isNotNull();
        assertThat(savedBranch.getOrganization().getId()).isEqualTo(org.getId());
        assertThat(savedBranch.getName()).isEqualTo("Kadıköy Polikliniği");
        assertThat(savedBranch.getTimeZone()).isEqualTo("Europe/Istanbul");

        List<Branch> branches = branchRepository.findByOrganizationId(org.getId());
        assertThat(branches).hasSize(1);
        assertThat(branches.getFirst().getName()).isEqualTo("Kadıköy Polikliniği");
    }

    @Test
    @DisplayName("Should persist department under branch and retrieve by branchId and name")
    void shouldPersistDepartmentUnderBranch() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Metropol Tıp"));
        Branch branch = branchRepository.saveAndFlush(new Branch(org, "Şişli Şubesi", "Europe/Istanbul", "Halaskargazi Cad."));
        Department dept = departmentRepository.saveAndFlush(new Department(branch, "Dahiliye"));

        assertThat(dept.getId()).isNotNull();
        assertThat(dept.getBranch().getId()).isEqualTo(branch.getId());
        assertThat(dept.getName()).isEqualTo("Dahiliye");

        Optional<Department> found = departmentRepository.findByBranchIdAndName(branch.getId(), "Dahiliye");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Dahiliye");
    }

    @Test
    @DisplayName("Should persist branch opening hours and retrieve by branchId")
    void shouldPersistBranchOpeningHours() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Avrasya Klinik"));
        Branch branch = branchRepository.saveAndFlush(new Branch(org, "Beşiktaş Şubesi", "Europe/Istanbul", "Barbaros Bulvarı"));

        BranchOpeningHours hours = new BranchOpeningHours(branch, (short) 1, LocalTime.of(9, 0), LocalTime.of(17, 0));
        BranchOpeningHours savedHours = branchOpeningHoursRepository.saveAndFlush(hours);

        assertThat(savedHours.getId()).isNotNull();
        assertThat(savedHours.getWeekday()).isEqualTo((short) 1);
        assertThat(savedHours.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(savedHours.getEndTime()).isEqualTo(LocalTime.of(17, 0));

        List<BranchOpeningHours> list = branchOpeningHoursRepository.findByBranchId(branch.getId());
        assertThat(list).hasSize(1);
    }
}
