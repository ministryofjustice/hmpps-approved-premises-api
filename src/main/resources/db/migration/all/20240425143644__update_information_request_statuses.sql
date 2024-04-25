UPDATE
    approved_premises_applications
SET
    status = 'REQUESTED_FURTHER_INFORMATION'
WHERE
        id IN (
        SELECT
            application_id
        from
            assessments
        where
                id IN (
                SELECT
                    assessment_id
                from
                    assessment_clarification_notes
                where
                    response is null
            )
    );