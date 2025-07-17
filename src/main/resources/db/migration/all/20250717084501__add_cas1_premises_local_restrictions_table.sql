CREATE TABLE cas1_premises_local_restrictions (
   id                                           UUID PRIMARY KEY NOT NULL,
   description                                  TEXT NOT NULL,
   created_at                                   TIMESTAMPTZ NOT NULL,
   created_by_user_id                           UUID NOT NULL,
   approved_premises_id                         UUID NOT NULL,
   archived                                     BOOLEAN NOT NULL DEFAULT FALSE,
   CONSTRAINT fk_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id),
   CONSTRAINT fk_approved_premises FOREIGN KEY (approved_premises_id) REFERENCES approved_premises (premises_id)
);
