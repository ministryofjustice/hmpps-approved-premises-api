-- ${flyway:timestamp}

--These are randomly generated names

INSERT INTO "users" (id, name, delius_username, delius_staff_identifier, probation_region_id, delius_staff_code) VALUES
    ('aa30f20a-84e3-4baa-bef0-3c9bd51879ad', 'Default User', 'JIMSNOWLDAP', 2500041001, '43606be0-9836-441d-9bc1-5586de9ac931', 'STAFFCODE'), -- South West
    ('f29b6402-b5f0-4f39-a66f-a2bb13eb5d6d', 'A. E2etester', 'APPROVEDPREMISESTESTUSER', 2500041002, 'ca979718-b15d-4318-9944-69aaff281cad', 'STAFFCODE'), -- East of England
    ('0621c5b0-0028-40b7-87fb-53ec65704314', 'T. Assessor', 'TEMPORARY-ACCOMMODATION-E2E-TESTER', 2500041003, 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE'), -- Kent, Surrey & Sussex
    ('7e36a89e-c69e-48b1-a5cf-a7c8949f432a', 'T. Referrer', 'TEMPORARY-ACCOMMODATION-E2E-REFERRER', 2500510725, 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE'), -- Kent, Surrey & Sussex
    ('045b71d3-9845-49b3-a79b-c7799a6bc7bc', 'Panesar Jaspal', 'panesar.jaspal', 2500054544, 'afee0696-8df3-4d9f-9d0c-268f17772e2c', 'STAFFCODE') -- Wales
    ('b5825da0-1553-4398-90ac-6a8e0c8a4cae', 'Tester Testy', 'TESTER.TESTY', 2500043547, 'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891', 'STAFFCODE'), -- North East
    ('695ba399-c407-4b66-aafc-e8835d72b8a7', 'Bernard Beaks', 'BERNARD.BEAKS', 2500057096, 'ca979718-b15d-4318-9944-69aaff281cad', 'STAFFCODE'), -- East of England
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
    '4d6500ff-0670-4a8e-8581-dff45292b2e4',
    'CAS1_ASSESSOR',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    'f78a5a6d-7d15-415c-b10f-91c0dff16753',
    'CAS1_MATCHER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    'b7f476af-0e7e-44ba-9c66-abe27d8c70ba',
    'CAS1_MANAGER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    '0354ff61-295a-4d06-b9e7-ae9fd53eba12',
    'CAS1_WORKFLOW_MANAGER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    'dd949e2b-5f9b-4560-b3bc-c3ad6793fb28',
    'CAS1_APPLICANT',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    '81e7c996-4e20-4c4f-94c4-1acd4e5bce33',
    'CAS1_ADMIN',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    '28f03d11-c430-4dea-e0e3-bec6ea67d2d9',
    'CAS1_ASSESSOR',
    '0621c5b0-0028-40b7-87fb-53ec65704314'
  ),
  (
    '8adb9e9b-fd29-475d-e603-bf231500a3e7',
    'CAS1_MATCHER',
    '0621c5b0-0028-40b7-87fb-53ec65704314'
  ),
  (
    '208f58d7-2979-4cea-acb9-1c54e9d66193',
    'CAS1_MANAGER',
    '0621c5b0-0028-40b7-87fb-53ec65704314'
  ),
  (
    '492d40dc-5554-4476-e8ad-1ec0f309450a',
    'CAS1_WORKFLOW_MANAGER',
    '0621c5b0-0028-40b7-87fb-53ec65704314'
  ),
  (
    '830d7aab-96e1-4056-be91-045e089378b8',
    'CAS1_APPLICANT',
    '0621c5b0-0028-40b7-87fb-53ec65704314'
  ),
  (
    '29ffb4f6-d72a-47e0-9ece-0797c5ff2169',
    'CAS1_ADMIN',
    '0621c5b0-0028-40b7-87fb-53ec65704314'
  ),
  (
    '38f7f97a-c9d0-4521-dbf1-79acafe8a20f',
    'CAS1_ASSESSOR',
    '045b71d3-9845-49b3-a79b-c7799a6bc7bc'
  ),
  (
    '26dd8587-20a3-4925-c6fa-db15c57b41dd',
    'CAS1_MATCHER',
    '045b71d3-9845-49b3-a79b-c7799a6bc7bc'
  ),
  (
    'b0392e1e-c200-46eb-e42c-04d89f48febb',
    'CAS1_MANAGER',
    '045b71d3-9845-49b3-a79b-c7799a6bc7bc'
  ),
  (
    '60b95db2-bd09-4d7d-ca29-356b962e99c6',
    'CAS1_WORKFLOW_MANAGER',
    '045b71d3-9845-49b3-a79b-c7799a6bc7bc'
  ),
  (
    '2ec8c941-18e1-4715-8b4c-2a0d51050dfa',
    'CAS1_APPLICANT',
    '045b71d3-9845-49b3-a79b-c7799a6bc7bc'
  ),
  (
    'c3205409-b5d8-4945-ed5f-8ee9dba4ebe3',
    'CAS1_ADMIN',
    '045b71d3-9845-49b3-a79b-c7799a6bc7bc'
  ) ON CONFLICT (id)
DO
  NOTHING;

-- Copy all roles from JIMSNOWLDAP to APPROVEDPREMISESTESTUSER
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
SELECT
  gen_random_uuid() AS id,
  role AS role,
  (SELECT id FROM users where delius_username='APPROVEDPREMISESTESTUSER') AS user_id
FROM
  "user_role_assignments"
WHERE
  "user_id" = (SELECT id FROM users where delius_username='JIMSNOWLDAP')
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
