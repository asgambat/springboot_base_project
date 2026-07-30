CREATE TABLE payment_idempotency (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_fingerprint VARCHAR(64) NOT NULL,
    transaction_id VARCHAR(255),
    payment_status VARCHAR(100),
    response_message VARCHAR(1000),
    completed_at TIMESTAMP WITH TIME ZONE
);