ALTER TABLE cas1_space_bookings ALTER COLUMN approved_premises_application_id DROP NOT NULL;
ALTER TABLE cas1_space_bookings ADD offline_application_id UUID NULL;

ALTER TABLE cas1_space_bookings ADD FOREIGN KEY (offline_application_id) REFERENCES offline_applications(id);

alter table cas1_space_bookings
    add constraint only_one_application_set
    check ((approved_premises_application_id is null) <> (offline_application_id is null));
