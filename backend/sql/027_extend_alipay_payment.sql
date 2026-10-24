USE shopagent;
SELECT DATABASE(), VERSION(), CURRENT_USER(), @@hostname, @@port;
SHOW COLUMNS FROM payment_attempts LIKE 'out_trade_no';
SHOW CREATE TABLE payment_attempts;

ALTER TABLE payment_attempts
    ADD COLUMN out_trade_no VARCHAR(64)
                                CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN provider_app_id VARCHAR(32)
                                CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN provider_seller_id VARCHAR(32)
                                CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN provider_trade_no VARCHAR(64)
                                CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN provider_paid_at DATETIME(3) NULL,
    DROP CHECK chk_payment_attempts_channel,
    ADD CONSTRAINT chk_payment_attempts_channel
        CHECK (channel IN ('MOCK', 'ALIPAY_SANDBOX')),
    ADD UNIQUE KEY uk_payment_attempts_out_trade (out_trade_no),
    ADD UNIQUE KEY uk_payment_attempts_provider_trade (
                                                       channel, provider_trade_no
        ),
    ADD CONSTRAINT chk_payment_attempts_provider_identity
        CHECK (
            (channel = 'MOCK'
                AND out_trade_no IS NULL
                AND provider_app_id IS NULL
                AND provider_seller_id IS NULL
                AND provider_trade_no IS NULL
                AND provider_paid_at IS NULL)
                OR
            (channel = 'ALIPAY_SANDBOX'
                AND out_trade_no IS NOT NULL
                AND CHAR_LENGTH(TRIM(out_trade_no)) > 0
                AND provider_app_id IS NOT NULL
                AND CHAR_LENGTH(TRIM(provider_app_id)) > 0
                AND provider_seller_id IS NOT NULL
                AND CHAR_LENGTH(TRIM(provider_seller_id)) > 0)
            ),
    ADD CONSTRAINT chk_payment_attempts_provider_success
        CHECK (
            channel = 'MOCK' OR status <> 'SUCCEEDED'
                OR (provider_trade_no IS NOT NULL
                AND CHAR_LENGTH(TRIM(provider_trade_no)) > 0
                AND provider_paid_at IS NOT NULL)
            );

SHOW CREATE TABLE payment_attempts;