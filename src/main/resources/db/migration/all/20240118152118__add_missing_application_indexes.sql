CREATE INDEX ON application_timeline_notes(application_id);

CREATE INDEX ON applications(created_by_user_id);

CREATE INDEX ON approved_premises_applications(probation_region_id);

CREATE INDEX ON placement_requests(application_id);
CREATE INDEX ON placement_requests(booking_id);
CREATE INDEX ON placement_requests(placement_application_id);
CREATE INDEX ON placement_requests(allocated_to_user_id);
CREATE INDEX ON placement_requests(assessment_id);