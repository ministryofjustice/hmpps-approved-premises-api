INSERT INTO placement_applications_automatic (id, application_id, submitted_at, expected_arrival_date)
SELECT
    gen_random_uuid (),
    approved_premises_applications.id,
    applications.submitted_at,
    approved_premises_applications.arrival_date
FROM
    approved_premises_applications INNER JOIN applications
    ON approved_premises_applications.id = applications.id

WHERE
    approved_premises_applications.arrival_date is not null and
    applications.submitted_at is not null and
    approved_premises_applications.id not in (
        select distinct application_id from placement_applications_automatic
    )