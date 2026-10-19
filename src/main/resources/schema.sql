-- Categories
CREATE TABLE IF NOT EXISTS tenants (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(160) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    timezone    VARCHAR(64) NOT NULL DEFAULT 'Asia/Tokyo',
    locale      VARCHAR(16) NOT NULL DEFAULT 'ja-JP',
    base_currency CHAR(3) NOT NULL DEFAULT 'JPY',
    fiscal_year_start_month SMALLINT NOT NULL DEFAULT 4 CHECK (fiscal_year_start_month BETWEEN 1 AND 12),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE tenants ADD COLUMN IF NOT EXISTS timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Tokyo';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS locale VARCHAR(16) NOT NULL DEFAULT 'ja-JP';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS base_currency CHAR(3) NOT NULL DEFAULT 'JPY';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS fiscal_year_start_month SMALLINT NOT NULL DEFAULT 4;

INSERT INTO tenants (id, name, status)
SELECT 1, 'デフォルト企業', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM tenants);
SELECT setval(pg_get_serial_sequence('tenants', 'id'), COALESCE((SELECT MAX(id) FROM tenants), 1), TRUE);

CREATE TABLE IF NOT EXISTS departments (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants(id),
    code        VARCHAR(40) NOT NULL,
    name        VARCHAR(120) NOT NULL,
    manager     VARCHAR(120),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS projects (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants(id),
    department_id BIGINT REFERENCES departments(id),
    code        VARCHAR(40) NOT NULL,
    name        VARCHAR(160) NOT NULL,
    owner       VARCHAR(120),
    start_date  DATE,
    end_date    DATE,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants(id),
    email       VARCHAR(254) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role        VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    auth_version INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, email)
);
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS email_verifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    purpose     VARCHAR(24) NOT NULL DEFAULT 'REGISTRATION',
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE email_verifications ADD COLUMN IF NOT EXISTS purpose VARCHAR(24) NOT NULL DEFAULT 'REGISTRATION';

CREATE TABLE IF NOT EXISTS user_scopes (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scope_type VARCHAR(24) NOT NULL CHECK (scope_type IN ('TENANT','DEPARTMENT','PROJECT')),
    scope_id BIGINT,
    UNIQUE (tenant_id, user_id, scope_type, scope_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_global ON users (LOWER(email));

CREATE TABLE IF NOT EXISTS spending_requests (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants(id),
    requester_id BIGINT NOT NULL REFERENCES users(id),
    department_id BIGINT REFERENCES departments(id),
    project_id  BIGINT REFERENCES projects(id),
    category_id BIGINT,
    title       VARCHAR(160) NOT NULL,
    purpose     VARCHAR(1000) NOT NULL,
    amount      NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    requested_date DATE NOT NULL,
    status      VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS approval_history (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants(id),
    request_id  BIGINT NOT NULL REFERENCES spending_requests(id) ON DELETE CASCADE,
    actor_id    BIGINT NOT NULL REFERENCES users(id),
    action      VARCHAR(24) NOT NULL,
    comment     VARCHAR(1000),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants(id),
    actor_id    BIGINT REFERENCES users(id),
    action      VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id   VARCHAR(80),
    details     VARCHAR(1000),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created ON audit_logs (tenant_id, created_at DESC);

CREATE TABLE IF NOT EXISTS budget_change_requests (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    budget_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL REFERENCES users(id),
    change_amount NUMERIC(12,2) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMP,
    review_comment VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounting_connections (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    provider VARCHAR(60) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'NOT_CONFIGURED',
    credential_ciphertext TEXT,
    cursor_value TEXT,
    last_success_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, provider)
);

CREATE TABLE IF NOT EXISTS accounting_sync_runs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    connection_id BIGINT NOT NULL REFERENCES accounting_connections(id),
    status VARCHAR(24) NOT NULL,
    fetched_count INT NOT NULL DEFAULT 0,
    imported_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS external_actuals (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    connection_id BIGINT NOT NULL REFERENCES accounting_connections(id),
    external_id VARCHAR(240) NOT NULL,
    category_id BIGINT,
    department_id BIGINT REFERENCES departments(id),
    project_id BIGINT REFERENCES projects(id),
    booked_on DATE NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    description VARCHAR(500),
    payload_hash VARCHAR(64),
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (connection_id, external_id)
);

-- Categories
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    color       VARCHAR(20)  DEFAULT '#6c757d',
    icon        VARCHAR(50),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE categories ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES tenants(id);
UPDATE categories SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE categories ALTER COLUMN tenant_id SET DEFAULT 1;
ALTER TABLE categories ALTER COLUMN tenant_id SET NOT NULL;

INSERT INTO categories (tenant_id, name, type, color, icon)
SELECT 1, seed.name, seed.type, seed.color, seed.icon
FROM (VALUES
    ('給与', 'INCOME', '#16a34a', 'bi-cash-stack'),
    ('売上', 'INCOME', '#0ea5e9', 'bi-graph-up'),
    ('人件費', 'EXPENSE', '#f97316', 'bi-people'),
    ('交通費', 'EXPENSE', '#8b5cf6', 'bi-train-front'),
    ('消耗品費', 'EXPENSE', '#64748b', 'bi-box-seam')
) AS seed(name, type, color, icon)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.tenant_id = 1 AND c.name = seed.name AND c.type = seed.type);

-- Transactions
CREATE TABLE IF NOT EXISTS transactions (
    id               BIGSERIAL PRIMARY KEY,
    category_id      BIGINT       REFERENCES categories(id) ON DELETE SET NULL,
    type             VARCHAR(20)  NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount           NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    description      VARCHAR(255),
    transaction_date DATE         NOT NULL DEFAULT CURRENT_DATE,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES departments(id);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS project_id BIGINT REFERENCES projects(id);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id);
UPDATE transactions SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE transactions ALTER COLUMN tenant_id SET DEFAULT 1;
ALTER TABLE transactions ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type);

-- Budgets (monthly per category)
CREATE TABLE IF NOT EXISTS budgets (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    year        INT          NOT NULL,
    month       INT          NOT NULL CHECK (month BETWEEN 1 AND 12),
    amount      NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (category_id, year, month)
);

ALTER TABLE budgets ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES departments(id);
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS project_id BIGINT REFERENCES projects(id);
UPDATE budgets SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE budgets ALTER COLUMN tenant_id SET DEFAULT 1;
ALTER TABLE budgets ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE budgets DROP CONSTRAINT IF EXISTS budgets_category_id_year_month_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_budgets_dimension_unique ON budgets
    (tenant_id, category_id, year, month, COALESCE(department_id, 0), COALESCE(project_id, 0));

CREATE INDEX IF NOT EXISTS idx_budgets_year_month ON budgets(year, month);
