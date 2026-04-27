ALTER TABLE categories DROP CONSTRAINT IF EXISTS uk_categories_name;

ALTER TABLE categories ADD COLUMN family_id UUID NULL;
ALTER TABLE categories ADD COLUMN owner_user_id UUID NULL;

ALTER TABLE categories
    ADD CONSTRAINT fk_categories_family_id
        FOREIGN KEY (family_id) REFERENCES families (id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE categories
    ADD CONSTRAINT fk_categories_owner_user_id
        FOREIGN KEY (owner_user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE categories ADD CONSTRAINT ck_categories_scope_exclusive
    CHECK (
        (family_id IS NULL AND owner_user_id IS NULL) OR
        (family_id IS NOT NULL AND owner_user_id IS NULL) OR
        (family_id IS NULL AND owner_user_id IS NOT NULL)
    );

CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_global_name_lower
    ON categories (lower(name::text))
    WHERE family_id IS NULL AND owner_user_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_family_name_lower
    ON categories (family_id, lower(name::text))
    WHERE family_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_owner_name_lower
    ON categories (owner_user_id, lower(name::text))
    WHERE owner_user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_categories_family_id ON categories (family_id) WHERE family_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_categories_owner_user_id ON categories (owner_user_id) WHERE owner_user_id IS NOT NULL;
