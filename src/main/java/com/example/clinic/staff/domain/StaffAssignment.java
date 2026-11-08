package com.example.clinic.staff.domain;

import com.example.clinic.common.domain.BaseEntity;
import com.example.clinic.identity.domain.StaffProfile;
import com.example.clinic.organization.domain.Branch;
import com.example.clinic.organization.domain.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Rich domain aggregate linking a medical staff member to a designated branch and department.
 * Fulfills FR-007, STA-02, and TC-007.
 */
@Entity
@Table(name = "staff_assignment")
public class StaffAssignment extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffProfile staff;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected StaffAssignment() {
        super();
    }

    public StaffAssignment(StaffProfile staff, Branch branch, Department department) {
        this(UUID.randomUUID(), staff, branch, department, true, Instant.now());
    }

    public StaffAssignment(UUID id, StaffProfile staff, Branch branch, Department department, boolean active, Instant createdAt) {
        super(id, createdAt);
        this.staff = Objects.requireNonNull(staff, "staff must not be null");
        this.branch = Objects.requireNonNull(branch, "branch must not be null");
        this.department = Objects.requireNonNull(department, "department must not be null");
        validateDepartmentBelongsToBranch(this.branch, this.department);
        this.active = active;
    }

    public static void validateDepartmentBelongsToBranch(Branch branch, Department department) {
        if (!department.getBranch().getId().equals(branch.getId())) {
            throw new IllegalArgumentException(
                    "Department " + department.getId() + " (" + department.getName() +
                            ") does not belong to branch " + branch.getId() + " (" + branch.getName() + ")"
            );
        }
    }

    public StaffProfile getStaff() {
        return staff;
    }

    public Branch getBranch() {
        return branch;
    }

    public Department getDepartment() {
        return department;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    @Override
    public String toString() {
        return "StaffAssignment{" +
                "id=" + getId() +
                ", staffId=" + (staff != null ? staff.getId() : null) +
                ", branchId=" + (branch != null ? branch.getId() : null) +
                ", departmentId=" + (department != null ? department.getId() : null) +
                ", active=" + active +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
