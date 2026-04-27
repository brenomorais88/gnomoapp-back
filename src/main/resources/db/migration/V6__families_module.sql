-- Family module: creator, lifecycle, member profile fields, membership statuses.

ALTER TABLE families
    ADD COLUMN IF NOT EXISTS created_by_user_id UUID NULL
        CONSTRAINT fk_families_created_by_user_id
            REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE families
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE families
    DROP CONSTRAINT IF EXISTS ck_families_status;

ALTER TABLE families
    ADD CONSTRAINT ck_families_status CHECK (status IN ('ACTIVE', 'ARCHIVED'));

ALTER TABLE family_members
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(200) NOT NULL DEFAULT '';

ALTER TABLE family_members
    ADD COLUMN IF NOT EXISTS document VARCHAR(32) NULL;

ALTER TABLE family_members
    ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL;

ALTER TABLE family_members
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20) NULL;

ALTER TABLE family_members
    ADD COLUMN IF NOT EXISTS invited_by_user_id UUID NULL
        CONSTRAINT fk_family_members_invited_by_user_id
            REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE family_members
    ADD COLUMN IF NOT EXISTS joined_at TIMESTAMPTZ NULL;

UPDATE family_members fm
SET document = fm.pending_match_document
WHERE fm.pending_match_document IS NOT NULL
    AND (fm.document IS NULL OR btrim(fm.document::text) = '');

UPDATE family_members
SET status = 'PENDING_REGISTRATION'
WHERE status = 'PENDING';

UPDATE family_members fm
SET display_name = trim(both ' ' FROM concat_ws(' ', u.first_name, u.last_name))
FROM users u
WHERE fm.user_id = u.id
    AND (fm.display_name IS NULL OR btrim(fm.display_name) = '');

UPDATE family_members
SET display_name = 'Member'
WHERE btrim(display_name) = '';

UPDATE families f
SET created_by_user_id = s.user_id
FROM (
    SELECT DISTINCT ON (family_id) family_id, user_id
    FROM family_members
    WHERE role = 'ADMIN'
        AND status = 'ACTIVE'
        AND user_id IS NOT NULL
    ORDER BY family_id, created_at ASC
) s
WHERE f.id = s.family_id
    AND f.created_by_user_id IS NULL;

ALTER TABLE family_members DROP CONSTRAINT IF EXISTS ck_family_members_pending_link;
ALTER TABLE family_members DROP CONSTRAINT IF EXISTS ck_family_members_status;

DROP INDEX IF EXISTS uk_family_members_family_user;
DROP INDEX IF EXISTS uk_family_members_one_user_row;
DROP INDEX IF EXISTS uk_family_members_family_pending_document;
DROP INDEX IF EXISTS idx_family_members_pending_doc;

ALTER TABLE family_members
    DROP COLUMN IF EXISTS pending_match_document;

ALTER TABLE family_members
    ADD CONSTRAINT ck_family_members_status
        CHECK (status IN ('PENDING_REGISTRATION', 'ACTIVE', 'REMOVED'));

ALTER TABLE family_members
    ADD CONSTRAINT ck_family_members_active_has_user
        CHECK (status <> 'ACTIVE' OR user_id IS NOT NULL);

CREATE UNIQUE INDEX IF NOT EXISTS uk_family_members_family_user_active
    ON family_members (family_id, user_id)
    WHERE user_id IS NOT NULL AND status IN ('ACTIVE', 'PENDING_REGISTRATION');

CREATE UNIQUE INDEX IF NOT EXISTS uk_family_members_one_user_active
    ON family_members (user_id)
    WHERE user_id IS NOT NULL AND status IN ('ACTIVE', 'PENDING_REGISTRATION');

CREATE UNIQUE INDEX IF NOT EXISTS uk_family_members_family_pending_document_active
    ON family_members (family_id, document)
    WHERE status = 'PENDING_REGISTRATION' AND document IS NOT NULL AND user_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_family_members_document_pending
    ON family_members (document)
    WHERE document IS NOT NULL AND status = 'PENDING_REGISTRATION' AND user_id IS NULL;
