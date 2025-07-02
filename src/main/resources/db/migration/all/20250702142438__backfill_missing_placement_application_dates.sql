UPDATE placement_applications
SET
    expected_arrival=placement_app_date.expected_arrival,
    duration=placement_app_date.duration
FROM (SELECT placement_application_id, expected_arrival, duration FROM placement_application_dates) AS placement_app_date
WHERE
    placement_applications.id = placement_app_date.placement_application_id AND
    placement_applications.submitted_at IS NOT NULL AND
    placement_applications.expected_arrival IS NULL;