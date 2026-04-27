CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    credit_limit DECIMAL(14,2),
    closing_day INT,
    due_day INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_cards_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

ALTER TABLE transactions ADD COLUMN card_id BIGINT;
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_card FOREIGN KEY (card_id) REFERENCES cards(id);
