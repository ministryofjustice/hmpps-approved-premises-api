-- change INTERVAL to 32 days
DROP VIEW IF EXISTS cas_2_application_live_summary CASCADE;
CREATE OR REPLACE VIEW cas_2_application_live_summary AS SELECT
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
    a.referring_prison_code,
    a.abandoned_at
FROM cas_2_application_summary a
WHERE (a.conditional_release_date IS NULL OR a.conditional_release_date >= current_date)
AND a.abandoned_at IS NULL
AND a.status_id IS NULL
   OR (a.status_id = '004e2419-9614-4c1e-a207-a8418009f23d' AND a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Referral withdrawn
   OR (a.status_id = 'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9' AND a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Referral cancelled
   OR (a.status_id = '89458555-3219-44a2-9584-c4f715d6b565' AND a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Awaiting arrival
   OR (a.status_id NOT IN ('004e2419-9614-4c1e-a207-a8418009f23d',
                           'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9',
                           '89458555-3219-44a2-9584-c4f715d6b565'));