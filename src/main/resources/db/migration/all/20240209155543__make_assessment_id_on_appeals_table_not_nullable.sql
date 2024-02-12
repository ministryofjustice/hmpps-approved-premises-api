UPDATE appeals
SET assessment_id = assessments.id
FROM assessments
JOIN (
	SELECT application_id, MAX(created_at) AS created_at
	FROM assessments
	GROUP BY application_id
) most_recent_assessments_by_application
ON assessments.application_id = most_recent_assessments_by_application.application_id
AND assessments.created_at = most_recent_assessments_by_application.created_at
WHERE appeals.application_id = assessments.application_id;

ALTER TABLE appeals ALTER COLUMN assessment_id SET NOT NULL;
