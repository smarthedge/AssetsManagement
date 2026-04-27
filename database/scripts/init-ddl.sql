-- ============================================================================
-- Assets Management System - Database Initialization Script
-- PostgreSQL DDL with audit fields, soft delete, and optimistic locking
-- ============================================================================

-- ----------------------------------------------------------------------------
-- USERS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username                    VARCHAR(50)     NOT NULL UNIQUE,
    email                       VARCHAR(100)    NOT NULL UNIQUE,
    password_hash               VARCHAR(255)    NOT NULL,
    status                      BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Audit fields
    created_by_user_id          BIGINT,
    created_by_username         VARCHAR(50),
    created_datetime            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_changed_by_user_id     BIGINT,
    last_changed_by_username    VARCHAR(50),
    last_changed_datetime       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Optimistic locking
    version                     INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_status   ON users (status);

-- ----------------------------------------------------------------------------
-- ROLES TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                        VARCHAR(50)     NOT NULL UNIQUE,
    description                 VARCHAR(255),
    status                      BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Audit fields
    created_by_user_id          BIGINT,
    created_by_username         VARCHAR(50),
    created_datetime            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_changed_by_user_id     BIGINT,
    last_changed_by_username    VARCHAR(50),
    last_changed_datetime       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Optimistic locking
    version                     INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_roles_name   ON roles (name);
CREATE INDEX idx_roles_status ON roles (status);

-- ----------------------------------------------------------------------------
-- USER_ROLES JOIN TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_roles (
    user_id                     BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id                     BIGINT          NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ----------------------------------------------------------------------------
-- ASSETS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS assets (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                        VARCHAR(100)    NOT NULL,
    description                 TEXT,
    category                    VARCHAR(50),
    serial_number               VARCHAR(100)    UNIQUE,
    purchase_date               DATE,
    value                       DECIMAL(12, 2),
    status                      BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Audit fields
    created_by_user_id          BIGINT,
    created_by_username         VARCHAR(50),
    created_datetime            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_changed_by_user_id     BIGINT,
    last_changed_by_username    VARCHAR(50),
    last_changed_datetime       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Optimistic locking
    version                     INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_assets_serial_number ON assets (serial_number);
CREATE INDEX idx_assets_category      ON assets (category);
CREATE INDEX idx_assets_status        ON assets (status);

-- ============================================================================
-- SEED DATA
-- ============================================================================

-- Seed roles
INSERT INTO roles (name, description, created_by_username, created_datetime, last_changed_by_username, last_changed_datetime)
VALUES
    ('ROLE_ADMIN', 'System administrator with full access', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
    ('ROLE_USER',  'Standard user with limited access',     'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Seed admin user (password: admin123 — BCrypt-hashed, change in production)
INSERT INTO users (username, email, password_hash, created_by_username, created_datetime, last_changed_by_username, last_changed_datetime)
VALUES
    ('admin', 'admin@assetsmanagement.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
VALUES (1, 1)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Seed sample assets
INSERT INTO assets (name, description, category, serial_number, purchase_date, value, created_by_username, created_datetime, last_changed_by_username, last_changed_datetime)
VALUES
    ('Dell Latitude 5540',     'Employee laptop - Engineering',        'Laptop',    'SN-2024-001', '2024-01-15', 1850.00, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
    ('MacBook Pro 16"',        'Design team workstation',              'Laptop',    'SN-2024-002', '2024-02-20', 2499.00, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
    ('iPhone 15 Pro',          'Company mobile device',                'Mobile',    'SN-2024-003', '2024-03-10', 1099.00, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
    ('Samsung 49" Monitor',    'Ultrawide curved monitor for devs',    'Monitor',   'SN-2024-004', '2024-01-10', 1299.00, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
    ('Logitech MX Keys',       'Wireless keyboard',                    'Peripheral','SN-2024-005', '2024-04-05', 119.99,  'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
    ('Cisco Catalyst Switch',  'Network switch - 48 port',             'Network',   'SN-2024-006', '2024-03-01', 2899.00, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
    ('HP LaserJet Pro',        'Office printer - 3rd floor',           'Printer',   'SN-2024-007', '2024-02-15', 549.00,  'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
    ('Dell PowerEdge R750',    'Production database server',           'Server',    'SN-2024-008', '2024-01-05', 8500.00, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
    ('Ergonomic Standing Desk','Adjustable height desk',               'Furniture', 'SN-2024-009', '2024-05-01', 799.00,  'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
    ('APC Smart-UPS 1500',     'Server room UPS battery backup',       'Power',     'SN-2024-010', '2024-01-05', 479.99,  'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP)
ON CONFLICT (serial_number) DO NOTHING;

-- Reset sequences to account for seeded data
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) FROM users));
SELECT setval('roles_id_seq', (SELECT COALESCE(MAX(id), 0) FROM roles));
SELECT setval('assets_id_seq', (SELECT COALESCE(MAX(id), 0) FROM assets));


SELECT *
from assets;

SELECT *
from users;

SELECT * from roles;

UPDATE assets 
set created_by_user_id=1, last_changed_by_user_id=1;
