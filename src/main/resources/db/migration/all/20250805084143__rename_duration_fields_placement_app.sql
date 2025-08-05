ALTER TABLE placement_applications ADD requested_duration int4 NULL;
ALTER TABLE placement_applications ADD authorised_duration int4 NULL;

UPDATE placement_applications
SET requested_duration = requested_duration_days,
    authorised_duration = authorised_duration_days;
