CREATE TABLE approved_premises_applications (
    id UUID NOT NULL,
    is_pipe_application BOOLEAN,
    is_womens_application BOOLEAN,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES applications(id)
);

INSERT INTO approved_premises_applications (id, is_pipe_application, is_womens_application)
    SELECT id, is_pipe_application, is_womens_application FROM applications;

CREATE TABLE temporary_accommodation_applications (
    id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES applications(id)
);

ALTER TABLE applications DROP COLUMN is_pipe_application;
ALTER TABLE applications DROP COLUMN is_womens_application;

ALTER TABLE applications ADD COLUMN service TEXT;

UPDATE applications SET service = 'approved-premises';

ALTER TABLE applications ALTER COLUMN service SET NOT NULL;
