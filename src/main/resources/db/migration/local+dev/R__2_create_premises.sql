-- ${flyway:timestamp}
TRUNCATE TABLE premises CASCADE;

insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('Address 1', 'd33006b7-55d9-4a8e-b722-5e18093dbcdf', 'd75ce5b8-fc07-494b-8950-a46a63ac377e', 'Something House', NULL, 'LA9 1DS', 'a02b7727-63aa-46f2-80f1-e0b05b31903c', 'temporary-accommodation', 'active', 30);
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('Address 2', 'ada106c7-e1fb-409a-a38e-0002ea8e7e45', '89faa462-7ea6-45ba-b169-93d947d20cae', 'Something Court', NULL, 'EC1 3XZ', '6b4a1308-17af-4c1a-a330-6005bec9e27b', 'temporary-accommodation', 'active', 10);
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('Address 3', '36c7b1f2-5a4b-467b-838c-2970c9c253cf', '7baa6fa4-d029-4be5-a5a9-d87f9bd35ce5', 'Something Place', NULL, 'SA1 1AF', 'afee0696-8df3-4d9f-9d0c-268f17772e2c', 'temporary-accommodation', 'active', 25);
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('1 Somewhere', '459eeaba-55ac-4a1f-bae2-bad810d4016b', '5283aca7-21bd-4ded-96ec-09cc6f76468e', 'Beckenham Road', 'Notes', 'GL56 0QQ', 'd73ae6b5-041e-4d44-b859-b8c77567d893', 'approved-premises', 'active', 20);
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('2 Somewhere', 'e03c82e9-f335-414a-87a0-866060397d4a', '7de4177b-9177-4c28-9bb6-5f5292619546', 'Bedford AP', 'Notes', 'GL56 0QQ', '0544d95a-f6bb-43f8-9be7-aae66e3bf244', 'approved-premises', 'active', 30);
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('Address 4', 'b6a87a7e-1b6a-4c96-b7ff-8c0dc414796d', 'f66f8c8d-e811-471a-9f4d-45d4046c6f79', 'Fourth Avenue', NULL, 'FY1 7VG', 'a02b7727-63aa-46f2-80f1-e0b05b31903c', 'temporary-accommodation', 'active', 0);

INSERT INTO approved_premises (premises_id, q_code, ap_code) VALUES
    ('459eeaba-55ac-4a1f-bae2-bad810d4016b', 'Q022', 'BCKNHAM'),
    ('e03c82e9-f335-414a-87a0-866060397d4a', 'Q005', 'BDFORD');

INSERT INTO temporary_accommodation_premises (premises_id, pdu) VALUES
    ('d33006b7-55d9-4a8e-b722-5e18093dbcdf', 'Cumbria'),
    ('ada106c7-e1fb-409a-a38e-0002ea8e7e45', 'Camden and Islington'),
    ('36c7b1f2-5a4b-467b-838c-2970c9c253cf', 'Swansea, Neath Port Talbot'),
    ('b6a87a7e-1b6a-4c96-b7ff-8c0dc414796d', 'North West Lancashire (Blackpool and lower tier LAs in Lancashire - Fylde, Wyre and Lancaster)');
