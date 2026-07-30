ALTER TABLE orders ALTER COLUMN amount DECIMAL(14, 2);

ALTER TABLE payment_idempotency
    ADD COLUMN processing_lease_until TIMESTAMP WITH TIME ZONE;

UPDATE payment_idempotency
SET processing_lease_until = COALESCE(completed_at, CURRENT_TIMESTAMP)
WHERE processing_lease_until IS NULL;

ALTER TABLE payment_idempotency
    ALTER COLUMN processing_lease_until SET NOT NULL;