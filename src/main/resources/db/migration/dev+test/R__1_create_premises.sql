-- ${flyway:timestamp}

TRUNCATE TABLE premises CASCADE;

insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('Address 1', 'd33006b7-55d9-4a8e-b722-5e18093dbcdf', 'd75ce5b8-fc07-494b-8950-a46a63ac377e', 'Something House', NULL, 'LA9 1DS', 'a02b7727-63aa-46f2-80f1-e0b05b31903c', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('Address 2', 'ada106c7-e1fb-409a-a38e-0002ea8e7e45', '89faa462-7ea6-45ba-b169-93d947d20cae', 'Something Court', NULL, 'EC1 3XZ', '6b4a1308-17af-4c1a-a330-6005bec9e27b', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('Address 3', '36c7b1f2-5a4b-467b-838c-2970c9c253cf', '7baa6fa4-d029-4be5-a5a9-d87f9bd35ce5', 'Something Place', NULL, 'SA1 1AF', 'afee0696-8df3-4d9f-9d0c-268f17772e2c', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('Address 4', 'b6a87a7e-1b6a-4c96-b7ff-8c0dc414796d', 'f66f8c8d-e811-471a-9f4d-45d4046c6f79', 'Fourth Avenue', NULL, 'FY1 7VG', 'a02b7727-63aa-46f2-80f1-e0b05b31903c', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING;

-- Premises for Temporary Accommodation E2E tests
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('1 Bishop St', 'd6447105-4bfe-4f1e-add7-4668e1ca28b0', '463bd776-6466-48b9-a4fa-d77b8fd869f5', 'BISHOP1', NULL, 'CT22 4BH', 'db82d408-d440-4eb5-960b-119cb33427cd', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING; -- Kent, Surrey & Sussex
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('2 Bishop St', 'e2543d2f-33a9-454b-ae15-03ca0475faa3', '463bd776-6466-48b9-a4fa-d77b8fd869f5', 'BISHOP2', NULL, 'CT22 4BH', 'db82d408-d440-4eb5-960b-119cb33427cd', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING; -- Kent, Surrey & Sussex
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('3 Bishop St', '0ad5999f-a07c-4605-b875-81d7a17e9f70', '463bd776-6466-48b9-a4fa-d77b8fd869f5', 'BISHOP3', NULL, 'CT22 4BH', 'db82d408-d440-4eb5-960b-119cb33427cd', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING; -- Kent, Surrey & Sussex
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('11 Pavilion Rise', '70a6046c-23fc-4a30-b151-582ffd509e6a', '0dd9476c-4058-4574-a35d-f846588b047c', 'PAVILION11', NULL, 'SS18 3PK', 'ca979718-b15d-4318-9944-69aaff281cad', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING; -- East of England
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('12 Pavilion Rise', '6aa177cb-617f-4abb-be46-056ea7e4a59d', '0dd9476c-4058-4574-a35d-f846588b047c', 'PAVILION12', NULL, 'SS18 3PK', 'ca979718-b15d-4318-9944-69aaff281cad', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING; -- East of England
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status") values ('13 Pavilion Rise', '773431cd-f560-4be8-9e6f-b582a4ebf204', '0dd9476c-4058-4574-a35d-f846588b047c', 'PAVILION13', NULL, 'SS18 3PK', 'ca979718-b15d-4318-9944-69aaff281cad', 'temporary-accommodation', 'active') ON CONFLICT (id) DO NOTHING; -- East of England

INSERT INTO temporary_accommodation_premises (premises_id, probation_delivery_unit_id) VALUES
    ('6970895f-028b-4de3-b542-33ac695a47cf', 'ba31df91-b4b2-4f15-8a1e-8a137c4c22a7'), -- Bristol and South Gloucestershire
    ('d33006b7-55d9-4a8e-b722-5e18093dbcdf', '18ac2803-6d81-455c-9211-c459f03a8db7'), -- Cumbria
    ('ada106c7-e1fb-409a-a38e-0002ea8e7e45', '876d7b90-ffb0-4281-96e2-49dd5937a654'), -- Camden and Islington
    ('36c7b1f2-5a4b-467b-838c-2970c9c253cf', 'f017624d-b939-47d1-89a8-f83435eddf68'), -- Swansea, Neath Port Talbot
    ('b6a87a7e-1b6a-4c96-b7ff-8c0dc414796d', '0c417f99-eecf-48dc-8d95-b6842adae0ff'), -- North West Lancashire (Blackpool and lower tier LAs in Lancashire - Fylde, Wyre and Lancaster)
    ('d6447105-4bfe-4f1e-add7-4668e1ca28b0', 'f97a944d-72d4-4423-8b15-2db8e57bb012'), -- East Kent (Swale, Ashford, Canterbury, Folkestone and Hythe, Thanet and Dover
    ('e2543d2f-33a9-454b-ae15-03ca0475faa3', 'f97a944d-72d4-4423-8b15-2db8e57bb012'), -- East Kent (Swale, Ashford, Canterbury, Folkestone and Hythe, Thanet and Dover
    ('0ad5999f-a07c-4605-b875-81d7a17e9f70', 'f97a944d-72d4-4423-8b15-2db8e57bb012'), -- East Kent (Swale, Ashford, Canterbury, Folkestone and Hythe, Thanet and Dover
    ('70a6046c-23fc-4a30-b151-582ffd509e6a', 'f6fd8e4d-50a4-438d-83b8-335c538b8b66'), -- Essex South (Basildon, Brentwood, Rochford and Castlepoint, Southend-on-Sea
    ('6aa177cb-617f-4abb-be46-056ea7e4a59d', 'f6fd8e4d-50a4-438d-83b8-335c538b8b66'), -- Essex South (Basildon, Brentwood, Rochford and Castlepoint, Southend-on-Sea
    ('773431cd-f560-4be8-9e6f-b582a4ebf204', 'f6fd8e4d-50a4-438d-83b8-335c538b8b66')  -- Essex South (Basildon, Brentwood, Rochford and Castlepoint, Southend-on-Sea
ON CONFLICT (premises_id) DO NOTHING;
