-- create new table for external users
CREATE TABLE external_users (
    id              UUID                        NOT NULL,
    username        TEXT                        NOT NULL,
    origin          TEXT                        NOT NULL,
    is_enabled      BOOLEAN                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE,

    PRIMARY KEY (id),
    UNIQUE (username)
);
