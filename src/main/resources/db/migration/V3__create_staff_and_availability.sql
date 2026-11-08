-- ============================================================================
-- V3__create_staff_and_availability.sql
-- Sprint S05: Staff Assignment, Service Eligibility, Weekly Availability & Exceptions
-- ============================================================================

-- 1. Staff Assignment Table (Links a staff profile to a branch and department)
CREATE TABLE staff_assignment (
    id UUID PRIMARY KEY,
    staff_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    department_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_assignment_staff FOREIGN KEY (staff_id)
        REFERENCES staff_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_assignment_branch FOREIGN KEY (branch_id)
        REFERENCES branch(id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_assignment_department FOREIGN KEY (department_id)
        REFERENCES department(id) ON DELETE CASCADE,
    CONSTRAINT uq_staff_branch_department UNIQUE (staff_id, branch_id, department_id)
);

CREATE INDEX idx_staff_assignment_staff ON staff_assignment(staff_id);
CREATE INDEX idx_staff_assignment_branch ON staff_assignment(branch_id);
CREATE INDEX idx_staff_assignment_active ON staff_assignment(active);

-- 2. Staff Service Eligibility Table (Links staff assignment to branch services they can perform)
CREATE TABLE staff_service_eligibility (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL,
    branch_service_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eligibility_assignment FOREIGN KEY (assignment_id)
        REFERENCES staff_assignment(id) ON DELETE CASCADE,
    CONSTRAINT fk_eligibility_branch_service FOREIGN KEY (branch_service_id)
        REFERENCES branch_service(id) ON DELETE CASCADE,
    CONSTRAINT uq_assignment_service UNIQUE (assignment_id, branch_service_id)
);

CREATE INDEX idx_eligibility_assignment ON staff_service_eligibility(assignment_id);
CREATE INDEX idx_eligibility_branch_service ON staff_service_eligibility(branch_service_id);

-- 3. Weekly Availability Table (Routine recurring weekly working hours)
CREATE TABLE weekly_availability (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL,
    weekday SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_weekly_availability_assignment FOREIGN KEY (assignment_id)
        REFERENCES staff_assignment(id) ON DELETE CASCADE,
    CONSTRAINT chk_weekly_weekday_range CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT chk_weekly_time_order CHECK (start_time < end_time)
);

CREATE INDEX idx_weekly_availability_assignment ON weekly_availability(assignment_id);
CREATE INDEX idx_weekly_availability_weekday ON weekly_availability(assignment_id, weekday);

-- 4. Availability Exception Table (Date-specific leave or overtime modifications)
CREATE TABLE availability_exception (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL,
    local_date DATE NOT NULL,
    kind VARCHAR(50) NOT NULL,
    start_time TIME,
    end_time TIME,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exception_assignment FOREIGN KEY (assignment_id)
        REFERENCES staff_assignment(id) ON DELETE CASCADE,
    CONSTRAINT chk_exception_kind_valid CHECK (kind IN ('FULL_DAY_LEAVE', 'PARTIAL_LEAVE', 'OVERTIME')),
    CONSTRAINT chk_exception_time_order CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time),
    CONSTRAINT chk_exception_kind_times CHECK (
        (kind = 'FULL_DAY_LEAVE') OR
        (kind IN ('PARTIAL_LEAVE', 'OVERTIME') AND start_time IS NOT NULL AND end_time IS NOT NULL)
    )
);

CREATE INDEX idx_exception_assignment_date ON availability_exception(assignment_id, local_date);
