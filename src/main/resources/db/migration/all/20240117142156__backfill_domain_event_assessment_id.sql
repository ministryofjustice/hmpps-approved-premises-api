UPDATE domain_events d
SET assessment_id = a.id
FROM assessments a
WHERE d.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED' AND
      d.assessment_id IS NULL AND
      a.application_id = d.application_id AND
      a.reallocated_at IS NULL AND
      a.decision IS NOT NULL AND
      a.submitted_at = d.occurred_at;

-- there are a few domain events in prod that don't match the above rule
-- for those we assign them a completed assessment related to the same
-- application when there is one and only one such assessment

UPDATE domain_events d
SET assessment_id = a.id
FROM assessments a
WHERE
    d.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED' AND
    d.assessment_id IS NULL AND
    a.application_id = d.application_id AND
    a.reallocated_at IS NULL AND
    a.decision IS NOT NULL AND
    d.id IN (
        SELECT d1.id
        FROM domain_events d1
        INNER JOIN assessments a1 ON a1.application_id = d1.application_id
        WHERE d1.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED' AND
            d1.assessment_id IS NULL AND
            a1.reallocated_at IS NULL AND
            a1.decision IS NOT NULL
        GROUP BY d1.id
        HAVING count(a1.id) = 1
    );
