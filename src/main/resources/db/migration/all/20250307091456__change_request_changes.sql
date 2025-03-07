CREATE TABLE cas1_change_request_reasons (
    id uuid NOT NULL,
    code text NOT NULL,
    change_request_type text NOT NULL,
    archived boolean NOT NULL,
    CONSTRAINT cas1_change_request_reasons_pk PRIMARY KEY (id),
    CONSTRAINT cas1_change_request_reasons_unique UNIQUE (code),
    CONSTRAINT cas1_change_request_reasons_pk_and_type_unique UNIQUE (id,change_request_type)
);

CREATE TABLE cas1_change_request_rejection_reasons (
  id uuid NOT NULL,
  code text NOT NULL,
  change_request_type text NOT NULL,
  archived boolean NOT NULL,
  CONSTRAINT cas1_change_request_rejection_reasons_pk PRIMARY KEY (id),
  CONSTRAINT cas1_change_request_rejection_reasons_unique UNIQUE (code),
  CONSTRAINT cas1_change_request_rejection_reasons_pk_and_type_unique UNIQUE (id,change_request_type)
);

DROP TABLE cas1_change_requests;

CREATE TABLE cas1_change_requests (
  id uuid NOT NULL,
  placement_request_id uuid NOT NULL,
  cas1_space_booking_id uuid NOT NULL,
  "type" text NOT NULL,
  request_json jsonb NOT NULL,
  cas1_change_request_reason_id uuid NOT NULL,
  decision text NULL,
  decision_json jsonb NULL,
  descision_made_by_user_id uuid NULL,
  cas1_change_request_rejection_reason_id uuid NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT cas1_change_requests_pk PRIMARY KEY (id),
  CONSTRAINT cas1_change_requests_cas1_space_bookings_fk FOREIGN KEY (cas1_space_booking_id) REFERENCES cas1_space_bookings(id),
  CONSTRAINT cas1_change_requests_cas1_change_request_reasons_fk FOREIGN KEY (cas1_change_request_reason_id, "type") REFERENCES cas1_change_request_reasons(id, change_request_type),
  CONSTRAINT cas1_change_requests_cas1_change_request_rejection_reasons_fk FOREIGN KEY (cas1_change_request_rejection_reason_id, "type") REFERENCES cas1_change_request_rejection_reasons(id, change_request_type),
  CONSTRAINT cas1_change_requests_users_fk FOREIGN KEY (descision_made_by_user_id) REFERENCES users(id),
  CONSTRAINT cas1_change_requests_placement_requests_fk FOREIGN KEY (placement_request_id) REFERENCES placement_requests(id)
);
