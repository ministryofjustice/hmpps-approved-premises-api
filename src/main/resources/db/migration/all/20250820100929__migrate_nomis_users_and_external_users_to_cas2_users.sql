ALTER TABLE cas_2_users ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE cas_2_users ADD COLUMN external_type TEXT;
ALTER TABLE cas_2_users ADD COLUMN nomis_account_type TEXT;

INSERT INTO cas_2_users as c
SELECT
    n.id,
    n.name,
    n.nomis_username as username,
    n.email,
    'NOMIS' as user_type,
    n.nomis_staff_id,
    n.active_caseload_id as active_nomis_caseload_id,
    null as delius_staff_code,
    null as delius_team_codes,
    n.is_enabled,
    n.is_active,
    now() as created_at,
    now() as updated_at,
    null as external_type,
    n.account_type as nomis_account_type
FROM nomis_users as n;

INSERT INTO cas_2_users as c
SELECT
    e.id,
    e.name,
    e.username as username,
    e.email,
    'EXTERNAL' as user_type,
    null as nomis_staff_id,
    null as active_nomis_caseload_id,
    null as delius_staff_code,
    null as delius_team_codes,
    e.is_enabled,
    true as is_active,
    now() as created_at,
    now() as updated_at,
    e.origin as external_type,
    null as nomis_account_type
FROM external_users as e;