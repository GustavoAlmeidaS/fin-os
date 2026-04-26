-- V3__analytics_foundation.sql
-- Migration for BI and Analytics Support

-- 1. Soft Deletes for Dimensions
ALTER TABLE accounts ADD COLUMN deleted_at timestamp;
ALTER TABLE categories ADD COLUMN deleted_at timestamp;
ALTER TABLE counterparties ADD COLUMN deleted_at timestamp;
ALTER TABLE tags ADD COLUMN deleted_at timestamp;

-- 2. Metadata for Transactions
ALTER TABLE transactions ADD COLUMN metadata jsonb;

-- 3. Analytics Views
-- v_fact_transactions
CREATE VIEW v_fact_transactions AS
SELECT
    t.id AS transaction_id,
    t.user_id,
    t.transaction_date,
    EXTRACT(YEAR FROM t.transaction_date) AS "year",
    EXTRACT(MONTH FROM t.transaction_date) AS "month",
    TO_CHAR(t.transaction_date, 'YYYY-MM') AS year_month,
    t.type AS transaction_type,
    t.status AS transaction_status,
    t.source AS transaction_source,
    t.amount,
    t.description,
    t.notes,
    t.import_batch_id,
    t.metadata,
    t.created_at,
    t.updated_at,
    
    -- Account Joins
    a.id AS account_id,
    a.name AS account_name,
    a.type AS account_type,
    
    -- Destination Account Joins (for transfers)
    da.id AS destination_account_id,
    da.name AS destination_account_name,
    
    -- Category Joins
    c.id AS category_id,
    c.name AS category_name,
    c.type AS category_type,
    
    -- Counterparty Joins
    cp.id AS counterparty_id,
    cp.name AS counterparty_name,
    
    a.currency,
    
    -- Boolean flags for easy BI filtering
    CASE WHEN t.type = 'INCOME' THEN TRUE ELSE FALSE END AS is_income,
    CASE WHEN t.type = 'EXPENSE' THEN TRUE ELSE FALSE END AS is_expense,
    CASE WHEN t.type = 'TRANSFER' THEN TRUE ELSE FALSE END AS is_transfer,
    CASE WHEN t.type = 'ADJUSTMENT' THEN TRUE ELSE FALSE END AS is_adjustment

FROM transactions t
JOIN accounts a ON t.account_id = a.id
LEFT JOIN accounts da ON t.destination_account_id = da.id
LEFT JOIN categories c ON t.category_id = c.id
LEFT JOIN counterparties cp ON t.counterparty_id = cp.id
WHERE a.deleted_at IS NULL;

-- v_monthly_cashflow
-- Ignores TRANSFER and ADJUSTMENT, only looks at actual operational income/expenses
CREATE VIEW v_monthly_cashflow AS
SELECT
    user_id,
    EXTRACT(YEAR FROM transaction_date) AS "year",
    EXTRACT(MONTH FROM transaction_date) AS "month",
    TO_CHAR(transaction_date, 'YYYY-MM') AS year_month,
    COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS total_income,
    COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS total_expense,
    COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) - COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS net_cashflow
FROM transactions
WHERE type IN ('INCOME', 'EXPENSE')
GROUP BY
    user_id,
    EXTRACT(YEAR FROM transaction_date),
    EXTRACT(MONTH FROM transaction_date),
    TO_CHAR(transaction_date, 'YYYY-MM');
