UPDATE placement_applications
SET
    decision_made_at = allocated_at
WHERE
    decision IS NOT NULL AND
    decision_made_at IS NULL;