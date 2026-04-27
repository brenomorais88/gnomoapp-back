-- Foundation for V2 identity, families and memberships (no changes to billing tables yet).

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(180) NOT NULL,
    document VARCHAR(32) NOT NULL,
    birth_date DATE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(255) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_document UNIQUE (document)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_lower
    ON users (lower(email))
    WHERE email IS NOT NULL AND btrim(email) <> '';

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone_digits
    ON users (phone)
    WHERE phone IS NOT NULL AND btrim(phone) <> '';

CREATE TABLE IF NOT EXISTS families (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS family_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL,
    user_id UUID NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    pending_match_document VARCHAR(32) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_family_members_family_id
        FOREIGN KEY (family_id) REFERENCES families (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_family_members_user_id
        FOREIGN KEY (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_family_members_role
        CHECK (role IN ('ADMIN', 'MEMBER')),
    CONSTRAINT ck_family_members_status
        CHECK (status IN ('PENDING', 'ACTIVE')),
    CONSTRAINT ck_family_members_pending_link
        CHECK (
            (status = 'ACTIVE' AND user_id IS NOT NULL)
            OR (
                status = 'PENDING'
                AND (
                    (user_id IS NULL AND pending_match_document IS NOT NULL)
                    OR (user_id IS NOT NULL)
                )
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_family_members_family_id ON family_members (family_id);
CREATE INDEX IF NOT EXISTS idx_family_members_user_id ON family_members (user_id);
CREATE INDEX IF NOT EXISTS idx_family_members_pending_doc ON family_members (pending_match_document)
    WHERE pending_match_document IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_family_members_family_user
    ON family_members (family_id, user_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_family_members_one_user_row
    ON family_members (user_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_family_members_family_pending_document
    ON family_members (family_id, pending_match_document)
    WHERE status = 'PENDING' AND pending_match_document IS NOT NULL;
