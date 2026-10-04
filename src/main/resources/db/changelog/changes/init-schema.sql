--liquibase formatted sql

--changeset init:1
CREATE TABLE expense_limits
(
    id                       BIGSERIAL PRIMARY KEY,
    account_number           VARCHAR(10)              NOT NULL,
    expense_category         VARCHAR(16)              NOT NULL,
    limit_sum                NUMERIC(15, 2)           NOT NULL,
    limit_currency_shortname VARCHAR(3)               NOT NULL DEFAULT 'USD',
    limit_datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_limit_category CHECK (LOWER(expense_category) IN ('product', 'service'))
);

CREATE INDEX idx_limits_acc_cat_dt
    ON expense_limits (account_number, expense_category, limit_datetime DESC);


CREATE TABLE currency_rates
(
    id            BIGSERIAL PRIMARY KEY,
    currency_pair VARCHAR(7)     NOT NULL,
    rate_date     DATE           NOT NULL,
    close_rate    NUMERIC(18, 6) NOT NULL,
    CONSTRAINT uq_pair_date UNIQUE (currency_pair, rate_date)
);

CREATE INDEX idx_rates_pair_date
    ON currency_rates (currency_pair, rate_date DESC);


CREATE TABLE transactions
(
    id                 BIGSERIAL PRIMARY KEY,
    account_from       VARCHAR(10)              NOT NULL,
    account_to         VARCHAR(10)              NOT NULL,
    currency_shortname VARCHAR(3)               NOT NULL,
    sum                NUMERIC(15, 2)           NOT NULL,
    expense_category   VARCHAR(16)              NOT NULL,
    datetime           TIMESTAMP WITH TIME ZONE NOT NULL,
    sum_usd            NUMERIC(15, 2)           NOT NULL,
    limit_exceeded     BOOLEAN                  NOT NULL DEFAULT FALSE,
    applied_limit_id   BIGINT,
    CONSTRAINT fk_tx_applied_limit FOREIGN KEY (applied_limit_id) REFERENCES expense_limits (id),
    CONSTRAINT chk_trans_category CHECK (LOWER(expense_category) IN ('product', 'service'))
);

CREATE INDEX idx_trans_acc_cat_dt
    ON transactions (account_from, expense_category, datetime);


CREATE TABLE limit_locks
(
    account_number   VARCHAR(10)              NOT NULL,
    expense_category VARCHAR(16)              NOT NULL,
    locked_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_limit_locks PRIMARY KEY (account_number, expense_category)
);