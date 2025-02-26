CREATE TABLE cas1_delius_booking_import (
    booking_id uuid NOT NULL,
    crn text NOT NULL,
    event_number text NOT NULL,
    key_worker_staff_code text NULL,
    key_worker_forename text NULL,
    key_worker_middle_name text NULL,
    key_worker_surname text NULL,
    departure_reason_code text NULL,
    move_on_category_code text NULL,
    move_on_category_description text NULL,
    expected_arrival_date date NOT NULL,
    arrival_date date NULL,
    expected_departure_date date NOT NULL,
    departure_date date NULL,
    non_arrival_date date NULL,
    non_arrival_contact_datetime timestamptz NULL,
    non_arrival_reason_code text NULL,
    non_arrival_reason_description text NULL,
    non_arrival_notes text NULL,
    CONSTRAINT cas1_delius_booking_import_pk PRIMARY KEY (booking_id)
);
