CREATE TABLE url_checks (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT NOT NULL,
    status_code INTEGER NOT NULL,
    h1 TEXT,
    title TEXT,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_url_checks_url
        FOREIGN KEY (url_id)
        REFERENCES urls (id)
        ON DELETE CASCADE
);