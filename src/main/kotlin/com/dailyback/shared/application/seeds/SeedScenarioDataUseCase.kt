package com.dailyback.shared.application.seeds

import com.dailyback.shared.infrastructure.database.DatabaseFactory
import org.jetbrains.exposed.sql.transactions.transaction

class SeedScenarioDataUseCase(
    private val databaseFactory: DatabaseFactory,
) {
    fun execute() {
        databaseFactory.connect()
        transaction {
            exec(
                """
                DELETE FROM account_occurrences
                WHERE account_id IN (
                    '10000000-0000-0000-0000-000000000001',
                    '10000000-0000-0000-0000-000000000002',
                    '10000000-0000-0000-0000-000000000003',
                    '10000000-0000-0000-0000-000000000004',
                    '10000000-0000-0000-0000-000000000005',
                    '10000000-0000-0000-0000-000000000006',
                    '10000000-0000-0000-0000-000000000007',
                    '10000000-0000-0000-0000-000000000008'
                );
                """.trimIndent(),
            )

            exec(
                """
                DELETE FROM accounts
                WHERE id IN (
                    '10000000-0000-0000-0000-000000000001',
                    '10000000-0000-0000-0000-000000000002',
                    '10000000-0000-0000-0000-000000000003',
                    '10000000-0000-0000-0000-000000000004',
                    '10000000-0000-0000-0000-000000000005',
                    '10000000-0000-0000-0000-000000000006',
                    '10000000-0000-0000-0000-000000000007',
                    '10000000-0000-0000-0000-000000000008'
                );
                """.trimIndent(),
            )

            exec(
                """
                DELETE FROM categories
                WHERE id IN (
                    '20000000-0000-0000-0000-000000000001',
                    '20000000-0000-0000-0000-000000000002',
                    '20000000-0000-0000-0000-000000000003',
                    '20000000-0000-0000-0000-000000000004',
                    '20000000-0000-0000-0000-000000000005'
                );
                """.trimIndent(),
            )

            exec(
                """
                INSERT INTO categories (id, name, color, family_id, owner_user_id, created_at, updated_at)
                VALUES
                    ('20000000-0000-0000-0000-000000000001', '[TMP] Housing', '#1F2937', NULL, NULL, NOW(), NOW()),
                    ('20000000-0000-0000-0000-000000000002', '[TMP] Utilities', '#2563EB', NULL, NULL, NOW(), NOW()),
                    ('20000000-0000-0000-0000-000000000003', '[TMP] Food', '#16A34A', NULL, NULL, NOW(), NOW()),
                    ('20000000-0000-0000-0000-000000000004', '[TMP] Transport', '#9333EA', NULL, NULL, NOW(), NOW()),
                    ('20000000-0000-0000-0000-000000000005', '[TMP] Leisure', '#EA580C', NULL, NULL, NOW(), NOW());
                """.trimIndent(),
            )

            exec(
                """
                INSERT INTO accounts (
                    id, title, base_amount, start_date, end_date, recurrence_type,
                    category_id, notes, active,
                    ownership_type, owner_user_id, family_id, created_by_user_id, responsible_member_id,
                    created_at, updated_at
                ) VALUES
                    (
                        '10000000-0000-0000-0000-000000000001',
                        '[TMP] Rent',
                        1800.00,
                        CURRENT_DATE - INTERVAL '24 months',
                        NULL,
                        'MONTHLY',
                        '20000000-0000-0000-0000-000000000001',
                        'Core fixed monthly cost',
                        TRUE,
                        'FAMILY',
                        NULL,
                        (SELECT id FROM families ORDER BY created_at LIMIT 1),
                        (SELECT id FROM users ORDER BY created_at LIMIT 1),
                        NULL,
                        NOW(),
                        NOW()
                    ),
                    (
                        '10000000-0000-0000-0000-000000000002',
                        '[TMP] Gym Membership',
                        89.90,
                        CURRENT_DATE - INTERVAL '6 months',
                        CURRENT_DATE + INTERVAL '6 months',
                        'MONTHLY',
                        '20000000-0000-0000-0000-000000000005',
                        'Monthly subscription with end date',
                        TRUE,
                        'FAMILY',
                        NULL,
                        (SELECT id FROM families ORDER BY created_at LIMIT 1),
                        (SELECT id FROM users ORDER BY created_at LIMIT 1),
                        NULL,
                        NOW(),
                        NOW()
                    ),
                    (
                        '10000000-0000-0000-0000-000000000003',
                        '[TMP] Weekly Groceries',
                        220.00,
                        CURRENT_DATE - INTERVAL '3 months',
                        NULL,
                        'WEEKLY',
                        '20000000-0000-0000-0000-000000000003',
                        'Weekly household groceries',
                        TRUE,
                        'FAMILY',
                        NULL,
                        (SELECT id FROM families ORDER BY created_at LIMIT 1),
                        (SELECT id FROM users ORDER BY created_at LIMIT 1),
                        NULL,
                        NOW(),
                        NOW()
                    ),
                    (
                        '10000000-0000-0000-0000-000000000004',
                        '[TMP] School Snack',
                        15.50,
                        CURRENT_DATE - INTERVAL '15 days',
                        CURRENT_DATE + INTERVAL '30 days',
                        'DAILY',
                        '20000000-0000-0000-0000-000000000003',
                        'Daily small recurring expense',
                        TRUE,
                        'FAMILY',
                        NULL,
                        (SELECT id FROM families ORDER BY created_at LIMIT 1),
                        (SELECT id FROM users ORDER BY created_at LIMIT 1),
                        NULL,
                        NOW(),
                        NOW()
                    ),
                    (
                        '10000000-0000-0000-0000-000000000005',
                        '[TMP] Car Repair',
                        950.00,
                        CURRENT_DATE - INTERVAL '20 days',
                        CURRENT_DATE - INTERVAL '20 days',
                        'UNIQUE',
                        '20000000-0000-0000-0000-000000000004',
                        'One-time maintenance expense',
                        TRUE,
                        'FAMILY',
                        NULL,
                        (SELECT id FROM families ORDER BY created_at LIMIT 1),
                        (SELECT id FROM users ORDER BY created_at LIMIT 1),
                        NULL,
                        NOW(),
                        NOW()
                    ),
                    (
                        '10000000-0000-0000-0000-000000000006',
                        '[TMP] Streaming Service',
                        39.90,
                        CURRENT_DATE - INTERVAL '4 months',
                        NULL,
                        'MONTHLY',
                        '20000000-0000-0000-0000-000000000005',
                        'Inactive recurring account',
                        FALSE,
                        'FAMILY',
                        NULL,
                        (SELECT id FROM families ORDER BY created_at LIMIT 1),
                        (SELECT id FROM users ORDER BY created_at LIMIT 1),
                        NULL,
                        NOW(),
                        NOW()
                    ),
                    (
                        '10000000-0000-0000-0000-000000000007',
                        '[TMP] Future Travel Reservation',
                        1200.00,
                        CURRENT_DATE + INTERVAL '10 days',
                        CURRENT_DATE + INTERVAL '10 days',
                        'UNIQUE',
                        '20000000-0000-0000-0000-000000000004',
                        'Future one-time planned expense',
                        TRUE,
                        'FAMILY',
                        NULL,
                        (SELECT id FROM families ORDER BY created_at LIMIT 1),
                        (SELECT id FROM users ORDER BY created_at LIMIT 1),
                        NULL,
                        NOW(),
                        NOW()
                    ),
                    (
                        '10000000-0000-0000-0000-000000000008',
                        '[TMP] Fee Exempt Account',
                        0.00,
                        CURRENT_DATE - INTERVAL '1 month',
                        NULL,
                        'MONTHLY',
                        '20000000-0000-0000-0000-000000000002',
                        'Zero amount recurring account',
                        TRUE,
                        'FAMILY',
                        NULL,
                        (SELECT id FROM families ORDER BY created_at LIMIT 1),
                        (SELECT id FROM users ORDER BY created_at LIMIT 1),
                        NULL,
                        NOW(),
                        NOW()
                    );
                """.trimIndent(),
            )

            exec(
                """
                INSERT INTO account_occurrences (
                    id, account_id, title_snapshot, amount_snapshot, due_date, status, paid_at,
                    notes_snapshot, category_id_snapshot, created_at, updated_at
                ) VALUES
                    (
                        '30000000-0000-0000-0000-000000000001',
                        '10000000-0000-0000-0000-000000000001',
                        '[TMP] Rent',
                        1800.00,
                        CURRENT_DATE - INTERVAL '1 month',
                        'PAID',
                        NOW() - INTERVAL '20 days',
                        'Core fixed monthly cost',
                        '20000000-0000-0000-0000-000000000001',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000002',
                        '10000000-0000-0000-0000-000000000001',
                        '[TMP] Rent',
                        1800.00,
                        CURRENT_DATE,
                        'PENDING',
                        NULL,
                        'Core fixed monthly cost',
                        '20000000-0000-0000-0000-000000000001',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000003',
                        '10000000-0000-0000-0000-000000000001',
                        '[TMP] Rent',
                        1900.00,
                        CURRENT_DATE + INTERVAL '1 month',
                        'PENDING',
                        NULL,
                        'Core fixed monthly cost (overridden value)',
                        '20000000-0000-0000-0000-000000000001',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000004',
                        '10000000-0000-0000-0000-000000000002',
                        '[TMP] Gym Membership',
                        89.90,
                        CURRENT_DATE - INTERVAL '10 days',
                        'PAID',
                        NOW() - INTERVAL '9 days',
                        'Monthly subscription with end date',
                        '20000000-0000-0000-0000-000000000005',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000005',
                        '10000000-0000-0000-0000-000000000002',
                        '[TMP] Gym Membership',
                        79.90,
                        CURRENT_DATE + INTERVAL '20 days',
                        'PENDING',
                        NULL,
                        'Discounted overridden amount',
                        '20000000-0000-0000-0000-000000000005',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000006',
                        '10000000-0000-0000-0000-000000000003',
                        '[TMP] Weekly Groceries',
                        220.00,
                        CURRENT_DATE - INTERVAL '7 days',
                        'PAID',
                        NOW() - INTERVAL '6 days',
                        'Weekly household groceries',
                        '20000000-0000-0000-0000-000000000003',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000007',
                        '10000000-0000-0000-0000-000000000003',
                        '[TMP] Weekly Groceries',
                        220.00,
                        CURRENT_DATE,
                        'PENDING',
                        NULL,
                        'Weekly household groceries',
                        '20000000-0000-0000-0000-000000000003',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000008',
                        '10000000-0000-0000-0000-000000000003',
                        '[TMP] Weekly Groceries',
                        220.00,
                        CURRENT_DATE + INTERVAL '7 days',
                        'PENDING',
                        NULL,
                        'Weekly household groceries',
                        '20000000-0000-0000-0000-000000000003',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000009',
                        '10000000-0000-0000-0000-000000000004',
                        '[TMP] School Snack',
                        15.50,
                        CURRENT_DATE - INTERVAL '1 day',
                        'PAID',
                        NOW() - INTERVAL '12 hours',
                        'Daily small recurring expense',
                        '20000000-0000-0000-0000-000000000003',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000010',
                        '10000000-0000-0000-0000-000000000004',
                        '[TMP] School Snack',
                        15.50,
                        CURRENT_DATE,
                        'PENDING',
                        NULL,
                        'Daily small recurring expense',
                        '20000000-0000-0000-0000-000000000003',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000011',
                        '10000000-0000-0000-0000-000000000004',
                        '[TMP] School Snack',
                        20.00,
                        CURRENT_DATE + INTERVAL '1 day',
                        'PENDING',
                        NULL,
                        'Manual override for special day',
                        '20000000-0000-0000-0000-000000000003',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000012',
                        '10000000-0000-0000-0000-000000000005',
                        '[TMP] Car Repair',
                        950.00,
                        CURRENT_DATE - INTERVAL '20 days',
                        'PAID',
                        NOW() - INTERVAL '19 days',
                        'One-time maintenance expense',
                        '20000000-0000-0000-0000-000000000004',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000013',
                        '10000000-0000-0000-0000-000000000006',
                        '[TMP] Streaming Service',
                        39.90,
                        CURRENT_DATE - INTERVAL '15 days',
                        'PAID',
                        NOW() - INTERVAL '14 days',
                        'Inactive recurring account',
                        '20000000-0000-0000-0000-000000000005',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000014',
                        '10000000-0000-0000-0000-000000000007',
                        '[TMP] Future Travel Reservation',
                        1200.00,
                        CURRENT_DATE + INTERVAL '10 days',
                        'PENDING',
                        NULL,
                        'Future one-time planned expense',
                        '20000000-0000-0000-0000-000000000004',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000015',
                        '10000000-0000-0000-0000-000000000008',
                        '[TMP] Fee Exempt Account',
                        0.00,
                        CURRENT_DATE - INTERVAL '1 day',
                        'PAID',
                        NOW() - INTERVAL '2 hours',
                        'Zero amount recurring account',
                        '20000000-0000-0000-0000-000000000002',
                        NOW(),
                        NOW()
                    ),
                    (
                        '30000000-0000-0000-0000-000000000016',
                        '10000000-0000-0000-0000-000000000008',
                        '[TMP] Fee Exempt Account',
                        0.00,
                        CURRENT_DATE + INTERVAL '29 days',
                        'PENDING',
                        NULL,
                        'Zero amount recurring account',
                        '20000000-0000-0000-0000-000000000002',
                        NOW(),
                        NOW()
                    );
                """.trimIndent(),
            )
        }
    }
}
