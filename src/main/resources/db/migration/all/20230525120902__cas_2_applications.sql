CREATE TABLE cas_2_applications (
    id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES applications(id)
);

ALTER TABLE cas_2_applications ADD COLUMN risk_ratings JSON;
