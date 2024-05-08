-- create application summary view
DROP VIEW IF EXISTS cas_2_application_summary;
CREATE VIEW cas_2_application_summary AS SELECT
    CAST(a.id AS TEXT),
    a.crn,
    a.noms_number,
    CAST(a.created_by_user_id AS TEXT),
    nu.name,
    a.created_at,
    a.submitted_at,
    a.hdc_eligibility_date,
    asu.label,
    CAST(asu.status_id AS TEXT),
    a.referring_prison_code,
    a.conditional_release_date
FROM cas_2_applications a
LEFT JOIN (SELECT DISTINCT ON (application_id) su.application_id, su.label, su.status_id
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
    ON a.id = asu.application_id
JOIN nomis_users nu ON nu.id = a.created_by_user_id;

-- create application summary view for live (i.e. unexpired) applications
DROP VIEW IF EXISTS cas_2_application_live_summary;
CREATE VIEW cas_2_application_live_summary AS SELECT
    a.id,
    a.crn,
    a.noms_number,
    a.created_by_user_id,
    a.name,
    a.created_at,
    a.submitted_at,
    a.hdc_eligibility_date,
    a.label,
    a.status_id,
    a.referring_prison_code
FROM cas_2_application_summary a
WHERE (a.conditional_release_date IS NULL OR a.conditional_release_date >= current_date);