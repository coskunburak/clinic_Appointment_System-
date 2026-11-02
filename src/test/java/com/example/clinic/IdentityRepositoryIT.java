package com.example.clinic;

import com.example.clinic.identity.domain.Membership;
import com.example.clinic.identity.domain.PatientProfile;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.identity.domain.UserAccount;
import com.example.clinic.identity.infrastructure.MembershipRepository;
import com.example.clinic.identity.infrastructure.PatientProfileRepository;
import com.example.clinic.identity.infrastructure.StaffProfileRepository;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Organization;
import com.example.clinic.organization.infrastructure.BranchRepository;
import com.example.clinic.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class IdentityRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    @DisplayName("Should persist and retrieve user account, ensuring password hash is not exposed via toString")
    void shouldPersistAndRetrieveUserAccount() {
        String testHash = "$2a$10$wE99K3f8.FakeBcryptHashForTestingOnlyNotRealSecret";
        UserAccount account = new UserAccount("doctor1@clinic.test", testHash);
        UserAccount saved = userAccountRepository.saveAndFlush(account);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("doctor1@clinic.test");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();

        // Security check: toString must never reveal the password hash
        assertThat(saved.toString()).doesNotContain(testHash);

        Optional<UserAccount> found = userAccountRepository.findByEmail("doctor1@clinic.test");
        assertThat(found).isPresent();
        assertThat(found.get().getPasswordHash()).isEqualTo(testHash);
    }

    @Test
    @DisplayName("Should persist membership scoped to organization and branch")
    void shouldPersistMembershipScoped() {
        Organization org = organizationRepository.saveAndFlush(new Organization("Global Sağlık"));
        Branch branch = branchRepository.saveAndFlush(new Branch(org, "Merkez Şube", "Europe/Istanbul", "Merkez Mah."));
        UserAccount user = userAccountRepository.saveAndFlush(new UserAccount("admin@clinic.test", "hash123"));

        Membership membership = new Membership(user, org, branch, "ROLE_ADMIN");
        Membership savedMembership = membershipRepository.saveAndFlush(membership);

        assertThat(savedMembership.getId()).isNotNull();
        assertThat(savedMembership.getRole()).isEqualTo("ROLE_ADMIN");
        assertThat(savedMembership.getUserAccount().getId()).isEqualTo(user.getId());
        assertThat(savedMembership.getOrganization().getId()).isEqualTo(org.getId());
        assertThat(savedMembership.getBranch().getId()).isEqualTo(branch.getId());

        List<Membership> memberships = membershipRepository.findByUserAccountId(user.getId());
        assertThat(memberships).hasSize(1);
    }

    @Test
    @DisplayName("Should persist patient profile linked to user account")
    void shouldPersistPatientProfile() {
        UserAccount user = userAccountRepository.saveAndFlush(new UserAccount("patient1@clinic.test", "hash123"));
        PatientProfile profile = new PatientProfile(user, "Ahmet Yılmaz", "+905551112233");
        PatientProfile savedProfile = patientProfileRepository.saveAndFlush(profile);

        assertThat(savedProfile.getId()).isNotNull();
        assertThat(savedProfile.getFullName()).isEqualTo("Ahmet Yılmaz");
        assertThat(savedProfile.getPhone()).isEqualTo("+905551112233");

        Optional<PatientProfile> found = patientProfileRepository.findByUserAccountId(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Ahmet Yılmaz");
    }

    @Test
    @DisplayName("Should persist staff profile linked to user account")
    void shouldPersistStaffProfile() {
        UserAccount user = userAccountRepository.saveAndFlush(new UserAccount("doctor_ayse@clinic.test", "hash123"));
        StaffProfile profile = new StaffProfile(user, "Dr. Ayşe Kaya", "+905554445566", "DOCTOR", "Kardiyoloji");
        StaffProfile savedProfile = staffProfileRepository.saveAndFlush(profile);

        assertThat(savedProfile.getId()).isNotNull();
        assertThat(savedProfile.getFullName()).isEqualTo("Dr. Ayşe Kaya");
        assertThat(savedProfile.getStaffType()).isEqualTo("DOCTOR");
        assertThat(savedProfile.getSpecialty()).isEqualTo("Kardiyoloji");
        assertThat(savedProfile.isActive()).isTrue();

        Optional<StaffProfile> found = staffProfileRepository.findByUserAccountId(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSpecialty()).isEqualTo("Kardiyoloji");
    }
}
