CREATE TABLE approved_premises_application_team_codes (
    id UUID NOT NULL,
    application_id UUID NOT NULL,
    team_code TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (application_id) REFERENCES applications(id),
    UNIQUE (application_id, team_code)
);
