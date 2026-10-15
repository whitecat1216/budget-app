-- Default categories (only if empty)
INSERT INTO categories (name, type, color, icon)
SELECT * FROM (VALUES
    ('給与', 'INCOME', '#28a745', 'salary'),
    ('ボーナス', 'INCOME', '#20c997', 'bonus'),
    ('その他収入', 'INCOME', '#17a2b8', 'other'),
    ('食費', 'EXPENSE', '#dc3545', 'food'),
    ('住居費', 'EXPENSE', '#fd7e14', 'home'),
    ('光熱費', 'EXPENSE', '#ffc107', 'utility'),
    ('交通費', 'EXPENSE', '#6f42c1', 'transport'),
    ('通信費', 'EXPENSE', '#e83e8c', 'communication'),
    ('娯楽', 'EXPENSE', '#6610f2', 'entertainment'),
    ('日用品', 'EXPENSE', '#20c997', 'daily'),
    ('医療', 'EXPENSE', '#dc3545', 'medical'),
    ('その他支出', 'EXPENSE', '#6c757d', 'other')
) AS v(name, type, color, icon)
WHERE NOT EXISTS (SELECT 1 FROM categories LIMIT 1);
