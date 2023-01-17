-- ${flyway:timestamp}
TRUNCATE TABLE users CASCADE;
--These are randomly generated names

INSERT INTO "users" (id, name, delius_username, delius_staff_identifier) VALUES
    ('b5825da0-1553-4398-90ac-6a8e0c8a4cae', 'Tester Testy', 'tester.testy', 2500043547),
    ('695ba399-c407-4b66-aafc-e8835d72b8a7', 'Bernard Beaks', 'bernard.beaks', 2500057096),
    ('045b71d3-9845-49b3-a79b-c7799a6bc7bc', 'Panesar Jaspal', 'panesar.jaspal', 2500054544);
