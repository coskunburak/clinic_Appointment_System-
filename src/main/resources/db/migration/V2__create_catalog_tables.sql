-- ==============================================================================
-- V2__create_catalog_tables.sql
-- Clinic Appointment Management System — Sprint S03 Catalog Schema
-- ==============================================================================

-- 1. Service Definition Table (Organization Scope)
CREATE TABLE service_definition (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_definition_organization FOREIGN KEY (organization_id)
        REFERENCES organization(id) ON DELETE RESTRICT,
    CONSTRAINT chk_service_definition_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT uq_service_definition_org_name UNIQUE (organization_id, name),
    CONSTRAINT uq_service_definition_id_org_id UNIQUE (id, organization_id)
);

CREATE INDEX idx_service_definition_org_id ON service_definition(organization_id);

-- 2. Branch Service Table (Branch-Specific Service Offering & Pricing)
CREATE TABLE branch_service (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    service_definition_id UUID NOT NULL,
    duration_minutes INTEGER NOT NULL,
    fee_amount NUMERIC(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'TRY',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_branch_service_organization FOREIGN KEY (organization_id)
        REFERENCES organization(id) ON DELETE RESTRICT,
    CONSTRAINT fk_branch_service_branch_org FOREIGN KEY (branch_id, organization_id)
        REFERENCES branch(id, organization_id) ON DELETE RESTRICT,
    CONSTRAINT fk_branch_service_def_org FOREIGN KEY (service_definition_id, organization_id)
        REFERENCES service_definition(id, organization_id) ON DELETE RESTRICT,
    CONSTRAINT uq_branch_service_unique UNIQUE (branch_id, service_definition_id),
    CONSTRAINT chk_branch_service_duration CHECK (duration_minutes >= 1 AND duration_minutes <= 480),
    CONSTRAINT chk_branch_service_fee CHECK (fee_amount >= 0),
    CONSTRAINT chk_branch_service_currency_length CHECK (char_length(trim(currency)) = 3)
);

CREATE INDEX idx_branch_service_branch ON branch_service(branch_id);
CREATE INDEX idx_branch_service_def ON branch_service(service_definition_id);
CREATE INDEX idx_branch_service_org ON branch_service(organization_id);
