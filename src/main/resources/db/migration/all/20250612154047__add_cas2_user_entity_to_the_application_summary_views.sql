-- First, drop the dependent views
DROP VIEW IF EXISTS cas_2_application_live_summary;
DROP VIEW IF EXISTS cas_2_application_summary;

-- Adding the most recent assignment data from the cas_2_application_assignments view
CREATE OR REPLACE VIEW cas_2_application_summary AS
SELECT a.id,
       a.crn,
       a.noms_number,
       a.created_by_user_id::text AS created_by_user_id,
       a.created_by_cas2_user_id::text AS created_by_cas2_user_id,
       nu.name,
       cu.name AS created_by_cas2_user_name,
       a.created_at,
       a.submitted_at,
       a.hdc_eligibility_date,
       asu.label,
       asu.status_id::text        AS status_id,
       a.referring_prison_code,
       a.conditional_release_date,
       a.bail_hearing_date,
       a.application_origin,
       asu.created_at             AS status_created_at,
       a.abandoned_at,
       aa.allocated_pom_user_id,
       nu2.name as allocated_pom_name,
       aa.created_at  as assignment_date,
       aa.prison_code as current_prison_code
FROM cas_2_applications a
         LEFT JOIN (SELECT DISTINCT ON (aa.application_id) *
                    FROM cas_2_application_assignments aa
                    ORDER BY aa.application_id, created_at DESC) aa ON a.id = aa.application_id

         LEFT JOIN (SELECT DISTINCT ON (su.application_id) su.application_id,
                                                           su.label,
                                                           su.status_id,
                                                           su.created_at
                    FROM cas_2_status_updates su
                    ORDER BY su.application_id, su.created_at DESC) asu ON a.id = asu.application_id
         LEFT JOIN nomis_users nu ON nu.id = a.created_by_user_id
         LEFT JOIN cas_2_users cu ON cu.id = a.created_by_cas2_user_id
         LEFT JOIN nomis_users nu2 ON nu2.id = aa.allocated_pom_user_id;


-- Adding the most recent assignment data from the cas_2_application_live_assignments view
CREATE OR REPLACE VIEW cas_2_application_live_summary AS
SELECT a.id,
       a.crn,
       a.noms_number,
       a.created_by_user_id,
       a.created_by_cas2_user_id::text AS created_by_cas2_user_id,
       a.name,
       a.name AS created_by_cas2_user_name,
       a.created_at,
       a.submitted_at,
       a.hdc_eligibility_date,
       a.label,
       a.status_id,
       a.referring_prison_code,
       a.abandoned_at,
       a.allocated_pom_user_id,
       a.allocated_pom_name,
       a.assignment_date,
       a.current_prison_code,
       a.bail_hearing_date,
       a.application_origin
FROM cas_2_application_summary a
WHERE (a.conditional_release_date IS NULL OR a.conditional_release_date >= CURRENT_DATE) AND a.abandoned_at IS NULL AND
      a.status_id IS NULL
   OR a.status_id = '004e2419-9614-4c1e-a207-a8418009f23d'::text AND
      a.status_created_at > (CURRENT_DATE - '32 days'::interval)
   OR a.status_id = 'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9'::text AND
      a.status_created_at > (CURRENT_DATE - '32 days'::interval)
   OR a.status_id = '89458555-3219-44a2-9584-c4f715d6b565'::text AND
      a.status_created_at > (CURRENT_DATE - '32 days'::interval)
   OR (a.status_id <> ALL
       (ARRAY ['004e2419-9614-4c1e-a207-a8418009f23d'::text, 'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9'::text, '89458555-3219-44a2-9584-c4f715d6b565'::text]))
