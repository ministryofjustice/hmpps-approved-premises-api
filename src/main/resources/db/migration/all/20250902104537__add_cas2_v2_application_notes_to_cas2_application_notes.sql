ALTER TABLE cas_2_application_notes DROP COLUMN created_by_nomis_user_id;
ALTER TABLE cas_2_application_notes DROP COLUMN created_by_external_user_id;
ALTER TABLE cas_2_application_notes
    DROP constraint IF EXISTS has_user;

ALTER TABLE cas_2_application_notes
    ADD CONSTRAINT has_user_cas2
        CHECK
            ((created_by_cas2_user_id is null)
            OR
             (created_by_cas2_user_id is not null));

INSERT INTO cas_2_application_notes as an2
SELECT
    an2v2.id,
    an2v2.application_id,
    an2v2.body,
    an2v2.created_at,
    an2v2.assessment_id,
    an2v2.created_by_user_id as created_by_cas2_user_id
FROM cas_2_v2_application_notes as an2v2;