USE shopagent;

SELECT DATABASE(), VERSION(), CURRENT_USER(), @@hostname, @@port;
SHOW TABLES LIKE 'payment_attempts';

CREATE TABLE payment_attempts (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  order_id BIGINT NOT NULL,

                                  request_key VARCHAR(36)
                                         CHARACTER SET ascii COLLATE ascii_bin NOT NULL,

                                  channel VARCHAR(32)
                                         CHARACTER SET ascii COLLATE ascii_bin
                                      NOT NULL DEFAULT 'MOCK',

                                  status VARCHAR(16)
                                         CHARACTER SET ascii COLLATE ascii_bin
                                      NOT NULL DEFAULT 'PENDING',

                                  amount DECIMAL(14, 2) NOT NULL,

                                  expires_at DATETIME(3) NOT NULL,
                                  finished_at DATETIME(3) NULL,

                                  created_at DATETIME(3) NOT NULL
                                               DEFAULT CURRENT_TIMESTAMP(3),

                                  updated_at DATETIME(3) NOT NULL
                                               DEFAULT CURRENT_TIMESTAMP(3)
                                      ON UPDATE CURRENT_TIMESTAMP(3),

                                  active_order_id BIGINT GENERATED ALWAYS AS (
                                      CASE WHEN status = 'PENDING'
                                               THEN order_id ELSE NULL END
                                      ) STORED,

                                  successful_order_id BIGINT GENERATED ALWAYS AS (
                                      CASE WHEN status = 'SUCCEEDED'
                                               THEN order_id ELSE NULL END
                                      ) STORED,

                                  PRIMARY KEY (id),

                                  UNIQUE KEY uk_payment_attempts_order_request (
                                                                                order_id, request_key
                                      ),

                                  UNIQUE KEY uk_payment_attempts_active (
                                                                         active_order_id
                                      ),

                                  UNIQUE KEY uk_payment_attempts_success (
                                                                          successful_order_id
                                      ),

                                  KEY idx_payment_attempts_order_id (order_id, id),

                                  KEY idx_payment_attempts_expiry (
                                                                   status, expires_at, id
                                      ),

                                  CONSTRAINT fk_payment_attempts_order
                                      FOREIGN KEY (order_id)
                                          REFERENCES shop_orders (id),

                                  CONSTRAINT chk_payment_attempts_request
                                      CHECK (CHAR_LENGTH(request_key) = 36),

                                  CONSTRAINT chk_payment_attempts_channel
                                      CHECK (channel = 'MOCK'),

                                  CONSTRAINT chk_payment_attempts_status
                                      CHECK (
                                          status IN (
                                                     'PENDING', 'SUCCEEDED', 'FAILED', 'CLOSED'
                                              )
                                          ),

                                  CONSTRAINT chk_payment_attempts_amount
                                      CHECK (amount > 0),

                                  CONSTRAINT chk_payment_attempts_expiry
                                      CHECK (expires_at > created_at),

                                  CONSTRAINT chk_payment_attempts_state_time
                                      CHECK (
                                          (status = 'PENDING' AND finished_at IS NULL)
                                              OR
                                          (
                                              status IN ('SUCCEEDED', 'FAILED', 'CLOSED')
                                                  AND finished_at IS NOT NULL
                                                  AND finished_at >= created_at
                                              )
                                          )
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

SHOW CREATE TABLE payment_attempts;