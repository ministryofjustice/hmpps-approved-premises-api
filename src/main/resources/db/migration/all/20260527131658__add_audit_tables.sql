CREATE TABLE revinfo
(
    id        INTEGER NOT NULL,
    timestamp BIGINT,
    username  TEXT,
    PRIMARY KEY (id)
);

CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE release_plan
(
    id                    UUID NOT NULL,
    space_booking_id      UUID NOT NULL,
    expected_release_time TIME,
    expected_arrival_time TIME,
    description           TEXT,
    other_information     TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_release_plan_space_booking FOREIGN KEY (space_booking_id) REFERENCES cas1_space_bookings (id)
);

CREATE TABLE release_plan_aud
(
    id                    UUID    NOT NULL,
    rev                   INTEGER NOT NULL,
    revtype               SMALLINT,
    space_booking_id      UUID,
    expected_release_time TIME,
    expected_arrival_time TIME,
    description           TEXT,
    other_information     TEXT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_release_plan_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (id)
);

CREATE TABLE release_action
(
    id                UUID NOT NULL,
    release_plan_id   UUID NOT NULL,
    description       TEXT NOT NULL,
    action_cadence    TEXT NOT NULL,
    other_information TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_release_action_release_plan FOREIGN KEY (release_plan_id) REFERENCES release_plan (id)
);

CREATE INDEX idx_release_action_release_plan_id
    ON release_action(release_plan_id);

CREATE TABLE release_action_aud
(
    id                UUID    NOT NULL,
    rev               INTEGER NOT NULL,
    revtype           SMALLINT,
    release_plan_id   UUID,
    description       TEXT,
    action_cadence    TEXT,
    other_information TEXT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_release_action_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (id)
);

CREATE TABLE release_plan_release_action_aud
(
    rev             INTEGER NOT NULL,
    release_plan_id UUID    NOT NULL,
    id              UUID    NOT NULL,
    revtype         SMALLINT,
    PRIMARY KEY (rev, release_plan_id, id),
    CONSTRAINT fk_release_plan_release_action_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (id)
);
