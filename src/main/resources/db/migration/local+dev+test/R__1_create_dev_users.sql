-- ${flyway:timestamp}

-- This should managed by the user seed job via 3__user.csv.
-- Note that the seed job will pull most user details (e.g. probation region, ap area id etc.) from delius when the user is created/updated
-- Also note that CAS1 specific users are added via the Cas1AutoScript. They could instead be loaded via an approved premises users seed job

INSERT INTO "users" (id, name, delius_username, probation_region_id, delius_staff_code, ap_area_id, created_at, cas1_cru_management_area_id) VALUES
    ('aa30f20a-84e3-4baa-bef0-3c9bd51879ad', 'Default User', 'JIMSNOWLDAP', '43606be0-9836-441d-9bc1-5586de9ac931', 'STAFFCODE', '2cc6bc0f-58cf-4d82-99dd-dfc67b7f5c33', now(), '667cc74b-60f9-4848-822b-2e8f7712cdf1'), -- Premises & AP: South West
    ('0621c5b0-0028-40b7-87fb-53ec65704314', 'T. Assessor', 'TEMPORARY-ACCOMMODATION-E2E-TESTER', 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE', '858a01cf-9fd1-482c-a983-1a74713f9227', now(), 'ddf23e67-053a-4384-b81b-48d576d3ceef'), -- Premises & AP: Kent, Surrey & Sussex
    ('7e36a89e-c69e-48b1-a5cf-a7c8949f432a', 'T. Referrer', 'TEMPORARY-ACCOMMODATION-E2E-REFERRER', 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE', '858a01cf-9fd1-482c-a983-1a74713f9227', now(), 'ddf23e67-053a-4384-b81b-48d576d3ceef'), -- Premises & AP: Kent, Surrey & Sussex
    ('b5825da0-1553-4398-90ac-6a8e0c8a4cae', 'Tester Testy', 'TESTER.TESTY', 'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891', 'STAFFCODE', '6025b8c6-883f-4300-bc26-2b01fbd6e272', now(), '64ad8602-5130-41da-bb2b-1c287b88fd90') -- Premises & AP: North East
ON CONFLICT (id) DO NOTHING;

-- assign CAS3_ASSESSOR and CAS3_REPORTER to TEMPORARY-ACCOMMODATION-E2E-TESTER
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
VALUES
  (
   '426b4846-ac0c-4e27-a350-873c5aaf62fd',
   'CAS3_ASSESSOR',
   (select id from users where delius_username = 'TEMPORARY-ACCOMMODATION-E2E-TESTER')
  ),
  (
  'f0ebbb07-dff5-4be6-97bf-7b393df12145',
  'CAS3_REPORTER',
  (select id from users where delius_username = 'TEMPORARY-ACCOMMODATION-E2E-TESTER')
  )
ON CONFLICT (id) DO NOTHING;
