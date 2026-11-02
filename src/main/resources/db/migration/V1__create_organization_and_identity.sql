-- ==============================================================================
-- V1__create_organization_and_identity.sql
-- Clinic Appointment Management System — Sprint S01 Baseline Schema
-- ==============================================================================

-- 1. Organization Table
CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_organization_name_not_blank CHECK (length(trim(name)) > 0)
);

-- 2. Branch Table
CREATE TABLE branch (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    time_zone VARCHAR(50) NOT NULL DEFAULT 'Europe/Istanbul',
    address VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_branch_organization FOREIGN KEY (organization_id)
        REFERENCES organization(id) ON DELETE RESTRICT,
    CONSTRAINT chk_branch_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_branch_time_zone_not_blank CHECK (length(trim(time_zone)) > 0),
    CONSTRAINT uq_branch_id_org_id UNIQUE (id, organization_id)
);

CREATE INDEX idx_branch_organization_id ON branch(organization_id);

-- 3. Branch Opening Hours Table
CREATE TABLE branch_opening_hours (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    weekday SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT fk_opening_hours_branch FOREIGN KEY (branch_id)
        REFERENCES branch(id) ON DELETE CASCADE,
    CONSTRAINT chk_opening_hours_weekday CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT chk_opening_hours_time_order CHECK (start_time < end_time)
);

CREATE INDEX idx_opening_hours_branch_id ON branch_opening_hours(branch_id);

-- 4. Department Table
CREATE TABLE department (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_department_branch FOREIGN KEY (branch_id)
        REFERENCES branch(id) ON DELETE RESTRICT,
    CONSTRAINT chk_department_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT uq_department_branch_name UNIQUE (branch_id, name)
);

CREATE INDEX idx_department_branch_id ON department(branch_id);

-- 5. User Account Table
CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_account_email UNIQUE (email),
    CONSTRAINT chk_user_account_email_not_blank CHECK (length(trim(email)) > 0),
    CONSTRAINT chk_user_account_hash_not_blank CHECK (length(trim(password_hash)) > 0)
);

-- 6. Membership Table (Scoped Authorization)
CREATE TABLE membership (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    branch_id UUID,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id)
        REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_organization FOREIGN KEY (organization_id)
        REFERENCES organization(id) ON DELETE RESTRICT,
    CONSTRAINT fk_membership_branch_org FOREIGN KEY (branch_id, organization_id)
        REFERENCES branch(id, organization_id) ON DELETE RESTRICT,
    CONSTRAINT chk_membership_role_not_blank CHECK (length(trim(role)) > 0)
);

CREATE INDEX idx_membership_user_id ON membership(user_id);
CREATE INDEX idx_membership_org_branch ON membership(organization_id, branch_id);

-- 7. Patient Profile Table
CREATE TABLE patient_profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_patient_profile_user_id UNIQUE (user_id),
    CONSTRAINT fk_patient_profile_user FOREIGN KEY (user_id)
        REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT chk_patient_full_name_not_blank CHECK (length(trim(full_name)) > 0)
);

-- 8. Staff Profile Table
CREATE TABLE staff_profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    staff_type VARCHAR(50) NOT NULL,
    specialty VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_staff_profile_user_id UNIQUE (user_id),
    CONSTRAINT fk_staff_profile_user FOREIGN KEY (user_id)
        REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT chk_staff_full_name_not_blank CHECK (length(trim(full_name)) > 0),
    CONSTRAINT chk_staff_type_not_blank CHECK (length(trim(staff_type)) > 0)
);

CREATE INDEX idx_staff_profile_active ON staff_profile(active);
