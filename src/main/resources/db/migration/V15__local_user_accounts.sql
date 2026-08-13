CREATE TABLE identity_user_accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    normalized_email VARCHAR(254) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_identity_user_accounts_normalized_email UNIQUE (normalized_email),
    CONSTRAINT ck_identity_user_accounts_role CHECK (role IN ('USER', 'EDITOR', 'ADMIN')),
    CONSTRAINT ck_identity_user_accounts_email_not_blank CHECK (btrim(email) <> ''),
    CONSTRAINT ck_identity_user_accounts_display_name_not_blank CHECK (btrim(display_name) <> '')
);

