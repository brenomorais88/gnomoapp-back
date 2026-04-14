INSERT INTO categories (name, color, created_at, updated_at)
VALUES
    ('Fixed', NULL, NOW(), NOW()),
    ('Health', NULL, NOW(), NOW()),
    ('Debt', NULL, NOW(), NOW()),
    ('Home', NULL, NOW(), NOW()),
    ('Transport', NULL, NOW(), NOW()),
    ('Food', NULL, NOW(), NOW()),
    ('Credit Card', NULL, NOW(), NOW()),
    ('Education', NULL, NOW(), NOW()),
    ('Others', NULL, NOW(), NOW())
ON CONFLICT (name) DO UPDATE SET updated_at = NOW();
