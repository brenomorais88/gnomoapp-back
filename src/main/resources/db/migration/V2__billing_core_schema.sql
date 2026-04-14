CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    color VARCHAR(20) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(180) NOT NULL,
    base_amount NUMERIC(14, 2) NOT NULL CHECK (base_amount >= 0),
    start_date DATE NOT NULL,
    end_date DATE NULL,
    recurrence_type VARCHAR(20) NOT NULL,
    category_id UUID NOT NULL,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_accounts_category_id
        FOREIGN KEY (category_id) REFERENCES categories (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_accounts_recurrence_type
        CHECK (recurrence_type IN ('UNIQUE', 'DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT ck_accounts_end_date
        CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE IF NOT EXISTS account_occurrences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    title_snapshot VARCHAR(180) NOT NULL,
    amount_snapshot NUMERIC(14, 2) NOT NULL CHECK (amount_snapshot >= 0),
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at TIMESTAMPTZ NULL,
    notes_snapshot TEXT NULL,
    category_id_snapshot UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_account_occurrences_account_id
        FOREIGN KEY (account_id) REFERENCES accounts (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_account_occurrences_category_id_snapshot
        FOREIGN KEY (category_id_snapshot) REFERENCES categories (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uk_account_occurrences_account_due_date UNIQUE (account_id, due_date),
    CONSTRAINT ck_account_occurrences_status
        CHECK (status IN ('OPEN', 'PAID', 'OVERDUE'))
);

CREATE INDEX IF NOT EXISTS idx_accounts_category_id ON accounts (category_id);
CREATE INDEX IF NOT EXISTS idx_account_occurrences_due_date ON account_occurrences (due_date);
CREATE INDEX IF NOT EXISTS idx_account_occurrences_status ON account_occurrences (status);
CREATE INDEX IF NOT EXISTS idx_account_occurrences_account_id ON account_occurrences (account_id);
CREATE INDEX IF NOT EXISTS idx_account_occurrences_category_id_snapshot ON account_occurrences (category_id_snapshot);
