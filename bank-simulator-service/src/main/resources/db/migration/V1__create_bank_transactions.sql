CREATE TABLE bank_transactions (

                                   payment_reference UUID PRIMARY KEY,

                                   amount NUMERIC(19, 2) NOT NULL,

                                   currency VARCHAR(3) NOT NULL,

                                   scenario VARCHAR(32) NOT NULL,

                                   status VARCHAR(16) NOT NULL,

                                   created_at TIMESTAMPTZ NOT NULL
                                       DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT chk_bank_amount
                                       CHECK (amount > 0),

                                   CONSTRAINT chk_bank_currency
                                       CHECK (currency = 'INR'),

                                   CONSTRAINT chk_bank_scenario
                                       CHECK (scenario IN (
                                                           'SUCCESS',
                                                           'DECLINED',
                                                           'TIMEOUT_AFTER_COMMIT'
                                           )),

                                   CONSTRAINT chk_bank_status
                                       CHECK (status IN ('APPROVED', 'DECLINED'))
);