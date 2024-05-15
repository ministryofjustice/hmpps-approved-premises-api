CREATE TABLE temporary_accommodation_application_prison_release_types (
    application_id UUID NOT NULL,
    prison_release_type_id UUID NOT NULL,
    PRIMARY KEY (application_id, prison_release_type_id),
    FOREIGN KEY (application_id) REFERENCES  applications(id),
    FOREIGN KEY (prison_release_type_id) REFERENCES prison_release_types(id)
)