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

CREATE INDEX IF NOT EXISTS idx_budgets_year_month ON budgets(year, month);
