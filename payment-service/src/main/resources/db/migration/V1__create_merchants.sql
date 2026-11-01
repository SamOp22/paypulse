CREATE TABLE merchants (

                           id UUID PRIMARY KEY,

                           name VARCHAR(120) NOT NULL,

                           email VARCHAR(255) NOT NULL UNIQUE,

                           password_hash VARCHAR(255) NOT NULL,

                           role VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',

                           created_at TIMESTAMPTZ NOT NULL
                                                     DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT chk_merchant_role
                               CHECK (role IN ('MERCHANT', 'ADMIN')),

                           CONSTRAINT chk_merchant_email_lower
                               CHECK (email = LOWER(email))
);
