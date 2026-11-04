DROP VIEW cas_2_application_live_summary;

CREATE VIEW cas_2_application_live_summary AS
SELECT a.id,
       a.crn,
       a.noms_number,
       a.created_by_cas2_user_id::text AS created_by_cas2_user_id,
       a.created_by_cas2_user_name AS created_by_cas2_user_name,
       a.created_at,
       a.submitted_at,
       a.hdc_eligibility_date,
       a.label,
       a.status_id,
       a.referring_prison_code,
       a.abandoned_at,
       a.allocated_pom_cas_2_user_id,
       a.allocated_pom_cas_2_name,
       a.assignment_date,
       a.current_prison_code,
       a.bail_hearing_date,
       a.application_origin,
       a.service_origin,
       a.cohort
FROM cas_2_application_summary a
WHERE (
          a.status_id IS NULL
          AND a.abandoned_at IS NULL
          AND (
              a.conditional_release_date IS NULL
              OR a.conditional_release_date >= CURRENT_DATE
              OR (a.cohort IS NULL OR a.cohort NOT IN ('HDC', 'PRISON_BAIL', 'COURT_BAIL'))
          )
      )
   OR (a.status_id = '004e2419-9614-4c1e-a207-a8418009f23d'::text AND
       a.status_created_at > (CURRENT_DATE - '32 days'::interval))
   OR (a.status_id = 'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9'::text AND
       a.status_created_at > (CURRENT_DATE - '32 days'::interval))
   OR (a.status_id = '89458555-3219-44a2-9584-c4f715d6b565'::text AND
       a.status_created_at > (CURRENT_DATE - '32 days'::interval))
   OR (a.status_id <> ALL
       (ARRAY ['004e2419-9614-4c1e-a207-a8418009f23d'::text, 'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9'::text, '89458555-3219-44a2-9584-c4f715d6b565'::text]));
