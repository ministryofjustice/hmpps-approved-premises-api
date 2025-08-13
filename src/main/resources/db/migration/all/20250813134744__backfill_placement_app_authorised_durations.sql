UPDATE placement_applications
SET authorised_duration = requested_duration
WHERE authorised_duration IS NULL
  AND id IN (
    SELECT placement_application_id
    FROM placement_requests
    WHERE placement_application_id IS NOT NULL
)
  AND decision = 'WITHDRAW';