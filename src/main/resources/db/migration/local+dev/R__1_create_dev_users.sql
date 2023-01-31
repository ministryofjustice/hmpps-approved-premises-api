-- ${flyway:timestamp}

--These are randomly generated names

INSERT INTO "users" (id, name, delius_username, delius_staff_identifier, probation_region_id) VALUES
    ('aa30f20a-84e3-4baa-bef0-3c9bd51879ad', 'Default User', 'JimSnowLdap', 2500041001, '43606be0-9836-441d-9bc1-5586de9ac931'), -- South West
    ('f29b6402-b5f0-4f39-a66f-a2bb13eb5d6d', 'A. E2etester', 'APPROVEDPREMISESTESTUSER', 2500041002, 'ca979718-b15d-4318-9944-69aaff281cad'), -- East of England
    ('0621c5b0-0028-40b7-87fb-53ec65704314', 'T. Tester', 'temporary-accommodation-e2e-tester', 2500041003, 'afee0696-8df3-4d9f-9d0c-268f17772e2c'), -- Wales
    ('b5825da0-1553-4398-90ac-6a8e0c8a4cae', 'Tester Testy', 'tester.testy', 2500043547, 'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891'), -- North East
    ('695ba399-c407-4b66-aafc-e8835d72b8a7', 'Bernard Beaks', 'bernard.beaks', 2500057096, 'ca979718-b15d-4318-9944-69aaff281cad'), -- East of England
    ('045b71d3-9845-49b3-a79b-c7799a6bc7bc', 'Panesar Jaspal', 'panesar.jaspal', 2500054544, 'afee0696-8df3-4d9f-9d0c-268f17772e2c') -- Wales
ON CONFLICT (id) DO NOTHING;
