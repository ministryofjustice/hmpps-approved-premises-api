ALTER TABLE
  placement_applications
ADD
  COLUMN "placement_type" text NULL;

create table
  placement_application_dates (
    "id" UUID primary key,
    "created_at" timestamp not null,
    "placement_application_id" UUID not null,
    "expected_arrival" DATE not null,
    "duration" INT4 not null,
    FOREIGN KEY (placement_application_id) REFERENCES placement_applications(id)
  )
