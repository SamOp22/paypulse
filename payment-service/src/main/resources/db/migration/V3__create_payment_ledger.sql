-- The longest payment status exceeds 20 characters.
ALTER TABLE payments
ALTER COLUMN status TYPE VARCHAR(32);


CREATE TABLE ledger_journals (

                                 id UUID PRIMARY KEY,

                                 payment_id UUID NOT NULL
                                     REFERENCES payments(id),

                                 reference_type VARCHAR(16) NOT NULL,

                                 reference_id UUID NOT NULL,

                                 event_type VARCHAR(32) NOT NULL,

                                 created_at TIMESTAMPTZ NOT NULL
                                     DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT chk_ledger_reference_type
                                     CHECK (reference_type IN ('CHARGE', 'REFUND')),

                                 CONSTRAINT chk_ledger_event_type
                                     CHECK (event_type IN (
                                                           'PAYMENT_CAPTURED',
                                                           'REFUND_POSTED'
                                         )),

                                 CONSTRAINT uq_ledger_reference
                                     UNIQUE (reference_type, reference_id)
);


CREATE TABLE ledger_entries (

                                id UUID PRIMARY KEY,

                                journal_id UUID NOT NULL
                                    REFERENCES ledger_journals(id),

                                account_code VARCHAR(40) NOT NULL,

                                direction VARCHAR(6) NOT NULL,

                                amount NUMERIC(19, 2) NOT NULL,

                                created_at TIMESTAMPTZ NOT NULL
                                    DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT chk_ledger_direction
                                    CHECK (direction IN ('DEBIT', 'CREDIT')),

                                CONSTRAINT chk_ledger_amount
                                    CHECK (amount > 0),

                                CONSTRAINT uq_ledger_journal_line
                                    UNIQUE (journal_id, account_code, direction)
);

CREATE INDEX idx_ledger_journals_payment
    ON ledger_journals(payment_id);