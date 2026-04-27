-- Add classification_rules table
CREATE TABLE classification_rules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_from_feedback BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_classification_rules_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_classification_rules_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_classification_rules_keyword ON classification_rules(keyword);
CREATE UNIQUE INDEX uk_classification_rules_keyword_category ON classification_rules(keyword, category_id, user_id);

-- Update transactions table for ETL fields
ALTER TABLE transactions ADD COLUMN idempotency_key VARCHAR(255);
ALTER TABLE transactions ADD COLUMN external_id VARCHAR(255);
ALTER TABLE transactions ADD COLUMN raw_description TEXT;
ALTER TABLE transactions ADD COLUMN searchable_description TEXT;
ALTER TABLE transactions ADD COLUMN installment_info VARCHAR(50);
ALTER TABLE transactions ADD COLUMN raw_row_payload TEXT;

-- For existing transactions, set idempotency_key as their ID so we can apply NOT NULL
UPDATE transactions SET idempotency_key = CAST(id AS VARCHAR) WHERE idempotency_key IS NULL;
UPDATE transactions SET raw_description = description WHERE raw_description IS NULL;
UPDATE transactions SET searchable_description = LOWER(description) WHERE searchable_description IS NULL;

ALTER TABLE transactions ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE transactions ALTER COLUMN raw_description SET NOT NULL;
ALTER TABLE transactions ALTER COLUMN searchable_description SET NOT NULL;

ALTER TABLE transactions ADD CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key);
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_import_batch FOREIGN KEY (import_batch_id) REFERENCES import_batches(id);
