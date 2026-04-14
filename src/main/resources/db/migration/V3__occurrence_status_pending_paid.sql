ALTER TABLE account_occurrences
    DROP CONSTRAINT IF EXISTS ck_account_occurrences_status;

UPDATE account_occurrences
SET status = 'PENDING'
WHERE status = 'OPEN';

ALTER TABLE account_occurrences
    ADD CONSTRAINT ck_account_occurrences_status
        CHECK (status IN ('PENDING', 'PAID'));
