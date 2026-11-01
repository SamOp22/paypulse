CREATE TABLE payments (

                          id UUID PRIMARY KEY,

                          merchant_id UUID NOT NULL,

                          amount NUMERIC(19, 2) NOT NULL,

                          currency VARCHAR(3) NOT NULL,

                          description VARCHAR(200),

                          status VARCHAR(20) NOT NULL DEFAULT 'CREATED',

                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_payments_merchant
                              FOREIGN KEY (merchant_id)
                                  REFERENCES merchants(id),

                          CONSTRAINT chk_payment_amount
                              CHECK (amount > 0),

                          CONSTRAINT chk_payment_currency
                              CHECK (currency = 'INR'),

                          CONSTRAINT chk_payment_status
                              CHECK (status IN (
                                                'CREATED',
                                                'PROCESSING',
                                                'SUCCESS',
                                                'FAILED',
                                                'REQUIRES_RECONCILIATION'
                                  ))
);

CREATE INDEX idx_payments_merchant_created
    ON payments(merchant_id, created_at DESC);


CREATE TABLE idempotency_records (

                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     merchant_id UUID NOT NULL,

                                     idempotency_key VARCHAR(128) NOT NULL,

                                     request_hash VARCHAR(64) NOT NULL,

                                     payment_id UUID,

                                     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_idempotency_merchant
                                         FOREIGN KEY (merchant_id)
                                             REFERENCES merchants(id),

                                     CONSTRAINT fk_idempotency_payment
                                         FOREIGN KEY (payment_id)
                                             REFERENCES payments(id),

                                     CONSTRAINT uq_merchant_idempotency
                                         UNIQUE (merchant_id, idempotency_key),

                                     CONSTRAINT uq_idempotency_payment
                                         UNIQUE (payment_id)
);
