-- Customizable permissions per family member (one row per member when configured).

CREATE TABLE IF NOT EXISTS family_member_permissions (
    member_id UUID PRIMARY KEY
        CONSTRAINT fk_family_member_permissions_member_id
            REFERENCES family_members (id) ON UPDATE CASCADE ON DELETE CASCADE,
    can_view_family_accounts BOOLEAN NOT NULL DEFAULT FALSE,
    can_create_family_accounts BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit_family_accounts BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete_family_accounts BOOLEAN NOT NULL DEFAULT FALSE,
    can_mark_family_accounts_paid BOOLEAN NOT NULL DEFAULT FALSE,
    can_manage_categories BOOLEAN NOT NULL DEFAULT FALSE,
    can_invite_members BOOLEAN NOT NULL DEFAULT FALSE,
    can_manage_members BOOLEAN NOT NULL DEFAULT FALSE,
    can_view_other_personal_accounts BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit_other_personal_accounts BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO family_member_permissions (
    member_id,
    can_view_family_accounts,
    can_create_family_accounts,
    can_edit_family_accounts,
    can_delete_family_accounts,
    can_mark_family_accounts_paid,
    can_manage_categories,
    can_invite_members,
    can_manage_members,
    can_view_other_personal_accounts,
    can_edit_other_personal_accounts
)
SELECT
    fm.id,
    TRUE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE
FROM family_members fm
WHERE fm.role = 'MEMBER'
    AND fm.status IN ('ACTIVE', 'PENDING_REGISTRATION')
    AND NOT EXISTS (
        SELECT 1 FROM family_member_permissions p WHERE p.member_id = fm.id
    );
