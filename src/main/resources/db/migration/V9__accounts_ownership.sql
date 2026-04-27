ALTER TABLE accounts ADD COLUMN ownership_type VARCHAR(20) NOT NULL DEFAULT 'FAMILY';
ALTER TABLE accounts ADD COLUMN owner_user_id UUID NULL;
ALTER TABLE accounts ADD COLUMN family_id UUID NULL;
ALTER TABLE accounts ADD COLUMN created_by_user_id UUID NULL;
ALTER TABLE accounts ADD COLUMN responsible_member_id UUID NULL;

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_owner_user_id
        FOREIGN KEY (owner_user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_family_id
        FOREIGN KEY (family_id) REFERENCES families (id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_created_by_user_id
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_responsible_member_id
        FOREIGN KEY (responsible_member_id) REFERENCES family_members (id) ON UPDATE CASCADE ON DELETE SET NULL;

UPDATE accounts
SET
    ownership_type = CASE
        WHEN EXISTS (SELECT 1 FROM families LIMIT 1) THEN 'FAMILY'
        ELSE 'PERSONAL'
    END,
    family_id = CASE
        WHEN EXISTS (SELECT 1 FROM families LIMIT 1) THEN (SELECT id FROM families ORDER BY created_at LIMIT 1)
        ELSE NULL
    END,
    owner_user_id = CASE
        WHEN EXISTS (SELECT 1 FROM families LIMIT 1) THEN NULL
        ELSE (SELECT id FROM users ORDER BY created_at LIMIT 1)
    END,
    created_by_user_id = (SELECT id FROM users ORDER BY created_at LIMIT 1)
WHERE EXISTS (SELECT 1 FROM users LIMIT 1);

UPDATE accounts
SET
    owner_user_id = (SELECT id FROM users ORDER BY created_at LIMIT 1),
    created_by_user_id = COALESCE(created_by_user_id, (SELECT id FROM users ORDER BY created_at LIMIT 1)),
    ownership_type = 'PERSONAL',
    family_id = NULL
WHERE ownership_type = 'FAMILY'
  AND family_id IS NULL
  AND EXISTS (SELECT 1 FROM users LIMIT 1);

ALTER TABLE accounts ADD CONSTRAINT ck_accounts_ownership_personal
    CHECK (ownership_type <> 'PERSONAL' OR (owner_user_id IS NOT NULL AND family_id IS NULL));

ALTER TABLE accounts ADD CONSTRAINT ck_accounts_ownership_family
    CHECK (ownership_type <> 'FAMILY' OR (family_id IS NOT NULL AND owner_user_id IS NULL));

CREATE INDEX IF NOT EXISTS idx_accounts_family_id ON accounts (family_id) WHERE family_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_accounts_owner_user_id ON accounts (owner_user_id) WHERE owner_user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_accounts_ownership_type ON accounts (ownership_type);
