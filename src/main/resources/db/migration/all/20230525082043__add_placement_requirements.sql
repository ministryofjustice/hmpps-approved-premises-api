CREATE TABLE placement_requirements (
    id UUID NOT NULL,
    gender TEXT NOT NULL,
    ap_type TEXT NOT NULL,
    postcode_district_id UUID NOT NULL,
    radius INTEGER NOT NULL,
    application_id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE
  placement_requirements_essential_criteria (
    placement_requirement_id UUID NOT NULL,
    characteristic_id UUID NOT NULL,
    PRIMARY KEY (placement_requirement_id, characteristic_id),
    FOREIGN KEY (placement_requirement_id) REFERENCES placement_requirements (id),
    FOREIGN KEY (characteristic_id) REFERENCES characteristics (id)
  );

CREATE TABLE
  placement_requirements_desirable_criteria (
    placement_requirement_id UUID NOT NULL,
    characteristic_id UUID NOT NULL,
    PRIMARY KEY (placement_requirement_id, characteristic_id),
    FOREIGN KEY (placement_requirement_id) REFERENCES placement_requirements (id),
    FOREIGN KEY (characteristic_id) REFERENCES characteristics (id)
  );

ALTER TABLE placement_requirements ADD CONSTRAINT postcode_district_id_fk FOREIGN KEY (postcode_district_id) REFERENCES postcode_districts(id);
ALTER TABLE placement_requirements ADD CONSTRAINT application_id_fk FOREIGN KEY (application_id) REFERENCES applications(id);
ALTER TABLE placement_requirements ADD CONSTRAINT assessment_id_fk FOREIGN KEY (assessment_id) REFERENCES assessments(id);

ALTER TABLE placement_requests ADD COLUMN placement_requirements_id UUID;
ALTER TABLE placement_requests ADD CONSTRAINT placement_requirements_id_fk FOREIGN KEY (placement_requirements_id) REFERENCES placement_requirements(id);
