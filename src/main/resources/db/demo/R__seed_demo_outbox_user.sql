INSERT INTO users (name, email, status, created_at, updated_at, version, deleted)
SELECT 'Demo Outbox User', 'demo-outbox@example.test', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, FALSE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'demo-outbox@example.test');