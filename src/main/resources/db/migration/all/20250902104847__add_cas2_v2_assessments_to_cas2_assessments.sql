ALTER TABLE cas_2_assessments ADD application_origin text NOT NULL DEFAULT 'homeDetentionCurfew';

INSERT INTO cas_2_assessments as a2
SELECT
    a2v2.id,
    a2v2.application_id,
    a2v2.nacro_referral_id,
    a2v2.assessor_name,
    a2v2.created_at,
    a.application_origin
FROM cas_2_v2_assessments as a2v2
JOIN cas_2_v2_applications as a on a.id = a2v2.application_id;