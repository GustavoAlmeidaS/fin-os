CREATE INDEX idx_transactions_searchable_desc ON transactions (searchable_description);

CREATE TABLE ai_rule_suggestion (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    suggested_category_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    import_batch_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    
    CONSTRAINT fk_ai_rule_sugg_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ai_rule_sugg_cat FOREIGN KEY (suggested_category_id) REFERENCES categories(id),
    CONSTRAINT fk_ai_rule_sugg_batch FOREIGN KEY (import_batch_id) REFERENCES import_batches(id)
);
