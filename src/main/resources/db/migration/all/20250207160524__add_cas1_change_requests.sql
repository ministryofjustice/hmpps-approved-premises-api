CREATE TABLE cas1_change_requests (
     id uuid NOT NULL,
     cas1_space_booking_id uuid NOT NULL,
     "type" text NOT NULL,
     requester_notes text NULL,
     created_by_user_id uuid NOT NULL,
     resolution text NULL,
     created_at timestamptz NOT NULL,
     updated_at timestamptz NOT NULL,
     resolved_at timestamptz NULL,
     CONSTRAINT cas1_change_requests_pk PRIMARY KEY (id),
     CONSTRAINT cas1_change_requests_cas1_space_bookings_fk FOREIGN KEY (cas1_space_booking_id) REFERENCES cas1_space_bookings(id),
     CONSTRAINT cas1_change_requests_users_fk FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);
CREATE INDEX cas1_change_requests_cas1_space_booking_id_idx ON cas1_change_requests (cas1_space_booking_id);
