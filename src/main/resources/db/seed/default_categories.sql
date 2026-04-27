INSERT INTO categories (name, color, family_id, owner_user_id, created_at, updated_at)
SELECT v.name, NULL::varchar, NULL::uuid, NULL::uuid, NOW(), NOW()
FROM (
    VALUES
        ('Fixed'),
        ('Health'),
        ('Debt'),
        ('Home'),
        ('Transport'),
        ('Food'),
        ('Credit Card'),
        ('Education'),
        ('Others')
) AS v(name)
WHERE NOT EXISTS (
    SELECT 1
    FROM categories c
    WHERE c.family_id IS NULL
      AND c.owner_user_id IS NULL
      AND lower(c.name) = lower(v.name)
);
