CREATE TABLE placement_requests (
    id UUID NOT NULL,
    gender TEXT NOT NULL,
    ap_type TEXT NOT NULL,
    expected_arrival DATE NOT NULL,
    duration INTEGER NOT NULL,
    postcode_district_id UUID NOT NULL,
    radius INTEGER NOT NULL,
    mental_health_support BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    application_id UUID NOT NULL,
    allocated_to_user_id UUID NOT NULL,
    booking_id UUID NULL,
    PRIMARY KEY (id)
);

CREATE TABLE
  placement_request_essential_criteria (
    placement_request_id UUID NOT NULL,
    characteristic_id UUID NOT NULL,
    PRIMARY KEY (placement_request_id, characteristic_id),
    FOREIGN KEY (placement_request_id) REFERENCES placement_requests (id),
    FOREIGN KEY (characteristic_id) REFERENCES characteristics (id)
  );

CREATE TABLE
  placement_request_desirable_criteria (
    placement_request_id UUID NOT NULL,
    characteristic_id UUID NOT NULL,
    PRIMARY KEY (placement_request_id, characteristic_id),
    FOREIGN KEY (placement_request_id) REFERENCES placement_requests (id),
    FOREIGN KEY (characteristic_id) REFERENCES characteristics (id)
  );

ALTER TABLE placement_requests ADD CONSTRAINT postcode_district_id_fk FOREIGN KEY (postcode_district_id) REFERENCES postcode_districts(id);
ALTER TABLE placement_requests ADD CONSTRAINT application_id_fk FOREIGN KEY (application_id) REFERENCES applications(id);
ALTER TABLE placement_requests ADD CONSTRAINT placement_requests_allocated_to_user_fk FOREIGN KEY (allocated_to_user_id) REFERENCES users(id);
ALTER TABLE placement_requests ADD CONSTRAINT booking_id_fk FOREIGN KEY (booking_id) REFERENCES bookings(id);