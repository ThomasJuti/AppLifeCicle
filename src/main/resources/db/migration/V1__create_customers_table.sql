CREATE TABLE IF NOT EXISTS customers (
    id         UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_customers      PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_email ON customers (email);
