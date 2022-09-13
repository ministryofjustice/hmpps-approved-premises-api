INSERT INTO ap_areas (id, identifier, "name") VALUES ('c482836a-bd2a-4cce-be6f-c5d548f621f8', 'LON', 'London');
INSERT INTO probation_regions (id, ap_area_id, "name") VALUES
    ('d73ae6b5-041e-4d44-b859-b8c77567d893', 'c482836a-bd2a-4cce-be6f-c5d548f621f8', 'London');


INSERT INTO ap_areas (id, identifier, "name") VALUES ('6025b8c6-883f-4300-bc26-2b01fbd6e272', 'NE', 'North East');
INSERT INTO probation_regions (id, ap_area_id, "name") VALUES
    ('c5acff6c-d0d2-4b89-9f4d-89a15cfa3891', '6025b8c6-883f-4300-bc26-2b01fbd6e272', 'North East'),
    ('5e44b880-df20-4751-938f-a14be5fe609d', '6025b8c6-883f-4300-bc26-2b01fbd6e272', 'Yorkshire & The Humber');


INSERT INTO ap_areas (id, identifier, "name") VALUES ('93a59a2d-1730-4154-8e02-aee6f2f01a76', 'NW', 'North West');
INSERT INTO probation_regions (id, ap_area_id, "name") VALUES
    ('a02b7727-63aa-46f2-80f1-e0b05b31903c', '93a59a2d-1730-4154-8e02-aee6f2f01a76', 'North West'),
    ('f6db2e41-040e-47c7-8bba-a345b6d35ca1', '93a59a2d-1730-4154-8e02-aee6f2f01a76', 'Greater Manchester');


INSERT INTO ap_areas (id, identifier, "name") VALUES ('37ceb2bf-c588-4f08-ab62-8f3e7f9b0c0f', 'Mids', 'Midlands');
INSERT INTO probation_regions (id, ap_area_id, "name") VALUES
    ('0544d95a-f6bb-43f8-9be7-aae66e3bf244', '37ceb2bf-c588-4f08-ab62-8f3e7f9b0c0f', 'East Midlands'),
    ('734261a0-d053-4aed-968d-ffc518cc17f8', '37ceb2bf-c588-4f08-ab62-8f3e7f9b0c0f', 'West Midlands');


INSERT INTO ap_areas (id, identifier, "name") VALUES ('858a01cf-9fd1-482c-a983-1a74713f9227', 'SEE', 'South East & Eastern');
INSERT INTO probation_regions (id, ap_area_id, "name") VALUES
    ('ca979718-b15d-4318-9944-69aaff281cad', '858a01cf-9fd1-482c-a983-1a74713f9227', 'East of England'),
    ('db82d408-d440-4eb5-960b-119cb33427cd', '858a01cf-9fd1-482c-a983-1a74713f9227', 'Kent, Surrey & Sussex');


INSERT INTO ap_areas (id, identifier, "name") VALUES ('2cc6bc0f-58cf-4d82-99dd-dfc67b7f5c33', 'SWSC', 'South West & South Central');
INSERT INTO probation_regions (id, ap_area_id, "name") VALUES
    ('43606be0-9836-441d-9bc1-5586de9ac931', '2cc6bc0f-58cf-4d82-99dd-dfc67b7f5c33', 'South West'),
    ('6b4a1308-17af-4c1a-a330-6005bec9e27b', '2cc6bc0f-58cf-4d82-99dd-dfc67b7f5c33', 'South Central');


INSERT INTO ap_areas (id, identifier, "name") VALUES ('56d78348-64d4-4af2-a13c-e5c0d4d30dd0', 'Wales', 'Wales');
INSERT INTO probation_regions (id, ap_area_id, "name") VALUES
    ('afee0696-8df3-4d9f-9d0c-268f17772e2c', '56d78348-64d4-4af2-a13c-e5c0d4d30dd0', 'Wales');
