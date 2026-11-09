-- ==============================================================================
-- V4__create_appointments.sql
-- Clinic Appointment Management System — Sprint S06 Schema
-- Appointment persistence, status audit history, and tenant integrity guard
-- ==============================================================================

-- 1. Tenant Integrity Guard: Ensure branch_service has a unique constraint on (id, branch_id)
-- to allow composite foreign key reference from appointment.
ALTER TABLE branch_service
    ADD CONSTRAINT uq_branch_service_id_branch_id UNIQUE (id, branch_id);

-- 2. Appointment Table
CREATE TABLE appointment (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    branch_service_id UUID NOT NULL,
    start_at_utc TIMESTAMPTZ NOT NULL,
    end_at_utc TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    service_name_snapshot VARCHAR(255) NOT NULL,
    duration_minutes_snapshot INTEGER NOT NULL,
    fee_amount_snapshot NUMERIC(12,2) NOT NULL,
    currency_snapshot CHAR(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id)
        REFERENCES patient_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_staff FOREIGN KEY (staff_id)
        REFERENCES staff_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_branch FOREIGN KEY (branch_id)
        REFERENCES branch(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_branch_service FOREIGN KEY (branch_service_id)
        REFERENCES branch_service(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_branch_service_branch FOREIGN KEY (branch_service_id, branch_id)
        REFERENCES branch_service(id, branch_id) ON DELETE RESTRICT,
    CONSTRAINT chk_appointment_time_order CHECK (start_at_utc < end_at_utc),
    CONSTRAINT chk_appointment_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_appointment_duration_positive CHECK (duration_minutes_snapshot >= 1 AND duration_minutes_snapshot <= 480),
    CONSTRAINT chk_appointment_fee_non_negative CHECK (fee_amount_snapshot >= 0),
    CONSTRAINT chk_appointment_currency_length CHECK (char_length(trim(currency_snapshot)) = 3),
    CONSTRAINT chk_appointment_service_name_not_blank CHECK (length(trim(service_name_snapshot)) > 0)
);

CREATE INDEX idx_appointment_staff_slot ON appointment(staff_id, start_at_utc, end_at_utc, status);
CREATE INDEX idx_appointment_patient_slot ON appointment(patient_id, start_at_utc, end_at_utc, status);
CREATE INDEX idx_appointment_branch ON appointment(branch_id);

-- 3. Appointment Status History Table (Append-only audit trail)
CREATE TABLE appointment_status_history (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    actor_user_id UUID NOT NULL,
    reason VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_status_history_appointment FOREIGN KEY (appointment_id)
        REFERENCES appointment(id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_actor FOREIGN KEY (actor_user_id)
        REFERENCES user_account(id) ON DELETE RESTRICT,
    CONSTRAINT chk_status_history_to_status CHECK (to_status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_status_history_from_status CHECK (from_status IS NULL OR from_status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED', 'COMPLETED'))
);

CREATE INDEX idx_status_history_appointment ON appointment_status_history(appointment_id);
