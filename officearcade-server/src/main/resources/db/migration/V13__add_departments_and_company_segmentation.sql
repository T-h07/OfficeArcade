CREATE TABLE departments (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    description VARCHAR(280) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_departments_code_ci
    ON departments (LOWER(code));

CREATE UNIQUE INDEX uq_departments_display_name_ci
    ON departments (LOWER(display_name));

CREATE INDEX idx_departments_active_display_name
    ON departments(active, display_name);

ALTER TABLE users
    ADD COLUMN department_id UUID NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_department
        FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL;

CREATE INDEX idx_users_department_id ON users(department_id);
