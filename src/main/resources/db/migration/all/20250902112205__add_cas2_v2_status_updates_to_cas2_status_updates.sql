ALTER TABLE cas_2_status_updates
    ADD CONSTRAINT fk_cas_2_assessor
        FOREIGN KEY(assessor_id)
            REFERENCES cas_2_users(id);

ALTER TABLE cas_2_status_updates
    DROP constraint IF EXISTS fk_assessor;

INSERT INTO cas_2_status_updates as s2
SELECT
    s2v2.id,
    s2v2.status_id,
    s2v2.application_id,
    s2v2.assessor_id,
    s2v2.description,
    s2v2.label,
    s2v2.created_at,
    s2v2.assessment_id
FROM cas_2_v2_status_updates as s2v2;