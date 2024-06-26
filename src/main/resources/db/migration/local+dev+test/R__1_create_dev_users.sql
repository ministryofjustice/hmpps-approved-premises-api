-- ${flyway:timestamp}

--These are randomly generated names

INSERT INTO "users" (id, name, delius_username, delius_staff_identifier, probation_region_id, delius_staff_code, ap_area_id) VALUES
    ('aa30f20a-84e3-4baa-bef0-3c9bd51879ad', 'Default User', 'JIMSNOWLDAP', 2500041001, '43606be0-9836-441d-9bc1-5586de9ac931', 'STAFFCODE', '2cc6bc0f-58cf-4d82-99dd-dfc67b7f5c33'), -- Premises & AP: South West
    ('9807de9d-05b3-49e2-84b8-529790a299cc', 'C. Load-Tester', 'CAS-LOAD-TESTER', 2500041004, 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE', '858a01cf-9fd1-482c-a983-1a74713f9227'), -- Premises & AP: Kent, Surrey & Sussex
    ('0621c5b0-0028-40b7-87fb-53ec65704314', 'T. Assessor', 'TEMPORARY-ACCOMMODATION-E2E-TESTER', 2500041003, 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE', '858a01cf-9fd1-482c-a983-1a74713f9227'), -- Premises & AP: Kent, Surrey & Sussex
    ('7e36a89e-c69e-48b1-a5cf-a7c8949f432a', 'T. Referrer', 'TEMPORARY-ACCOMMODATION-E2E-REFERRER', 2500510725, 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE', '858a01cf-9fd1-482c-a983-1a74713f9227'), -- Premises & AP: Kent, Surrey & Sussex
    ('b5825da0-1553-4398-90ac-6a8e0c8a4cae', 'Tester Testy', 'TESTER.TESTY', 2500043547, 'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891', 'STAFFCODE', '6025b8c6-883f-4300-bc26-2b01fbd6e272'), -- Premises & AP: North East
    ('695ba399-c407-4b66-aafc-e8835d72b8a7', 'Bernard Beaks', 'BERNARD.BEAKS', 2500057096, 'ca979718-b15d-4318-9944-69aaff281cad', 'STAFFCODE', '858a01cf-9fd1-482c-a983-1a74713f9227'), -- Premises & AP: East of England
    ('045b71d3-9845-49b3-a79b-c7799a6bc7bc', 'Panesar Jaspal', 'PANESAR.JASPAL', 2500054544, 'afee0696-8df3-4d9f-9d0c-268f17772e2c', 'STAFFCODE', '56d78348-64d4-4af2-a13c-e5c0d4d30dd0') -- Premises & AP: Wales
ON CONFLICT (id) DO NOTHING;

UPDATE
  users
SET
  "delius_username" = 'JIMSNOWLDAP'
WHERE
  id = 'aa30f20a-84e3-4baa-bef0-3c9bd51879ad';

INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
VALUES
  (
    'f1118a4e-bb6f-4b16-83ad-08ef92314610',
    'CAS3_ASSESSOR',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    '77c6cc54-5d01-41d2-9987-5b293d6d2e07',
    'CAS3_REFERRER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    '482a4ab7-a6e4-4bf8-a0fc-8516eca9efa2',
    'CAS3_REPORTER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  )
   ON CONFLICT (id)
DO
  NOTHING;

-- Copy all roles from JIMSNOWLDAP to TEMPORARY-ACCOMMODATION-E2E-TESTER
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
SELECT
  gen_random_uuid() AS id,
  role AS role,
  (SELECT id FROM users where delius_username='TEMPORARY-ACCOMMODATION-E2E-TESTER') AS user_id
FROM
  "user_role_assignments"
WHERE
  "user_id" = (SELECT id FROM users where delius_username='JIMSNOWLDAP')
  AND ("role" = 'CAS3_ASSESSOR' OR "role" = 'CAS3_REPORTER')
ON CONFLICT (id)
DO
  NOTHING;

-- Copy all roles from JIMSNOWLDAP to TESTER.TESTY
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
SELECT
  gen_random_uuid() AS id,
  role AS role,
  (SELECT id FROM users where delius_username='TESTER.TESTY') AS user_id
FROM
  "user_role_assignments"
WHERE
  "user_id" = (SELECT id FROM users where delius_username='JIMSNOWLDAP')
ON CONFLICT (id)
DO
  NOTHING;

-- Copy all roles from JIMSNOWLDAP to BERNARD.BEAKS
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
SELECT
  gen_random_uuid() AS id,
  role AS role,
  (SELECT id FROM users where delius_username='BERNARD.BEAKS') AS user_id
FROM
  "user_role_assignments"
WHERE
  "user_id" = (SELECT id FROM users where delius_username='JIMSNOWLDAP')
ON CONFLICT (id)
DO
  NOTHING;

-- Copy all roles from JIMSNOWLDAP to PANESAR.JASPAL
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
SELECT
  gen_random_uuid() AS id,
  role AS role,
  (SELECT id FROM users where delius_username='PANESAR.JASPAL') AS user_id
FROM
  "user_role_assignments"
WHERE
  "user_id" = (SELECT id FROM users where delius_username='JIMSNOWLDAP')
ON CONFLICT (id)
DO
  NOTHING;

-- Copy all roles from JIMSNOWLDAP to CAS-LOAD-TESTER
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
SELECT
  gen_random_uuid() AS id,
  role AS role,
  (SELECT id FROM users where delius_username='CAS-LOAD-TESTER') AS user_id
FROM
  "user_role_assignments"
WHERE
  "user_id" = (SELECT id FROM users where delius_username='JIMSNOWLDAP')
ON CONFLICT (id)
DO
  NOTHING;
