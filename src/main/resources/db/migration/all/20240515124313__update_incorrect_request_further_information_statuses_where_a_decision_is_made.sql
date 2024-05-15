-- Applications where the assessment is inapplicable -> `INAPPLICABLE`
UPDATE approved_premises_applications
SET status = 'PENDING_PLACEMENT_REQUEST'
WHERE status = 'REQUESTED_FURTHER_INFORMATION'
AND is_inapplicable = true;

-- Applications where the assessment is withdrawn -> `WITHDRAWN`
UPDATE approved_premises_applications
SET status = 'PENDING_PLACEMENT_REQUEST'
WHERE id IN (
    SELECT assessments.application_id
    FROM assessments
    INNER JOIN bookings ON bookings.application_id = assessments.application_id
    WHERE assessments.decision = 'ACCEPTED'
    AND assessments.reallocated_at IS NULL
)
AND status = 'REQUESTED_FURTHER_INFORMATION'
AND is_withdrawn = true;

-- Applications where a booking exists -> `PLACEMENT_ALLOCATED`
UPDATE approved_premises_applications
SET status = 'PLACEMENT_ALLOCATED'
WHERE id IN (
    SELECT assessments.application_id
    FROM assessments
    INNER JOIN bookings ON bookings.application_id = assessments.application_id
    WHERE assessments.decision = 'ACCEPTED'
    AND assessments.reallocated_at IS NULL
)
AND status = 'REQUESTED_FURTHER_INFORMATION';

-- Applications where a placement application exists with an `ACCEPTED` status -> `AWAITING_PLACEMENT`
UPDATE approved_premises_applications
SET status = 'AWAITING_PLACEMENT'
WHERE id IN (
    SELECT assessments.application_id
    FROM assessments
    INNER JOIN placement_applications ON placement_applications.application_id = assessments.application_id
    WHERE assessments.decision = 'ACCEPTED'
    AND assessments.reallocated_at IS NULL
    AND placement_applications.decision = 'ACCEPTED'
)
AND status = 'REQUESTED_FURTHER_INFORMATION';

-- Applications where a placement request exists and the application has an arrival date -> `AWAITING_PLACEMENT`
UPDATE approved_premises_applications
SET status = 'AWAITING_PLACEMENT'
WHERE id IN (
    SELECT assessments.application_id
    FROM assessments
    INNER JOIN placement_requests ON placement_requests.assessment_id = assessments.id
    AND placement_requests.application_id = assessments.application_id
    WHERE assessments.decision = 'ACCEPTED'
    AND assessments.reallocated_at IS NULL
)
AND status = 'REQUESTED_FURTHER_INFORMATION'
AND arrival_date IS NOT NULL;

-- Other applications where the assessment has been accepted -> `PENDING PLACEMENT REQUEST`
UPDATE approved_premises_applications
SET status = 'PENDING_PLACEMENT_REQUEST'
WHERE id IN (
    SELECT application_id
    FROM assessments
    WHERE decision = 'ACCEPTED'
    AND reallocated_at IS NULL
)
AND status = 'REQUESTED_FURTHER_INFORMATION';

-- Applications where the assessment has been rejected -> `REJECTED`
UPDATE approved_premises_applications
SET status = 'REJECTED'
WHERE id IN (
    SELECT application_id
    FROM assessments
    WHERE decision = 'REJECTED'
    AND reallocated_at IS NULL
)
AND status = 'REQUESTED_FURTHER_INFORMATION';
