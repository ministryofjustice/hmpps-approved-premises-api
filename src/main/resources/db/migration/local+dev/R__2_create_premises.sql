-- ${flyway:timestamp}

TRUNCATE TABLE premises CASCADE;

insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('Address 1', 'd33006b7-55d9-4a8e-b722-5e18093dbcdf', 'd75ce5b8-fc07-494b-8950-a46a63ac377e', 'Something House', NULL, 'LA9 1DS', 'a02b7727-63aa-46f2-80f1-e0b05b31903c', 'temporary-accommodation', 'active', 30) ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('Address 2', 'ada106c7-e1fb-409a-a38e-0002ea8e7e45', '89faa462-7ea6-45ba-b169-93d947d20cae', 'Something Court', NULL, 'EC1 3XZ', '6b4a1308-17af-4c1a-a330-6005bec9e27b', 'temporary-accommodation', 'active', 10) ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('Address 3', '36c7b1f2-5a4b-467b-838c-2970c9c253cf', '7baa6fa4-d029-4be5-a5a9-d87f9bd35ce5', 'Something Place', NULL, 'SA1 1AF', 'afee0696-8df3-4d9f-9d0c-268f17772e2c', 'temporary-accommodation', 'active', 25) ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('Address 4', 'b6a87a7e-1b6a-4c96-b7ff-8c0dc414796d', 'f66f8c8d-e811-471a-9f4d-45d4046c6f79', 'Fourth Avenue', NULL, 'FY1 7VG', 'a02b7727-63aa-46f2-80f1-e0b05b31903c', 'temporary-accommodation', 'active', 0) ON CONFLICT (id) DO NOTHING;

-- Premises for Temporary Accommodation E2E tests
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('1 Bishop St', 'd6447105-4bfe-4f1e-add7-4668e1ca28b0', '463bd776-6466-48b9-a4fa-d77b8fd869f5', 'BISHOP1', NULL, 'CT22 4BH', 'db82d408-d440-4eb5-960b-119cb33427cd', 'temporary-accommodation', 'active', 0) ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('2 Bishop St', 'e2543d2f-33a9-454b-ae15-03ca0475faa3', '463bd776-6466-48b9-a4fa-d77b8fd869f5', 'BISHOP2', NULL, 'CT22 4BH', 'db82d408-d440-4eb5-960b-119cb33427cd', 'temporary-accommodation', 'active', 0) ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('3 Bishop St', '0ad5999f-a07c-4605-b875-81d7a17e9f70', '463bd776-6466-48b9-a4fa-d77b8fd869f5', 'BISHOP3', NULL, 'CT22 4BH', 'db82d408-d440-4eb5-960b-119cb33427cd', 'temporary-accommodation', 'active', 0) ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('11 Pavilion Rise', '70a6046c-23fc-4a30-b151-582ffd509e6a', '0dd9476c-4058-4574-a35d-f846588b047c', 'PAVILION11', NULL, 'SS18 3PK', 'ca979718-b15d-4318-9944-69aaff281cad', 'temporary-accommodation', 'active', 0) ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('12 Pavilion Rise', '6aa177cb-617f-4abb-be46-056ea7e4a59d', '0dd9476c-4058-4574-a35d-f846588b047c', 'PAVILION12', NULL, 'SS18 3PK', 'ca979718-b15d-4318-9944-69aaff281cad', 'temporary-accommodation', 'active', 0) ON CONFLICT (id) DO NOTHING;
insert into "premises" ("address_line1", "id", "local_authority_area_id", "name", "notes", "postcode", "probation_region_id", "service", "status", "total_beds") values ('13 Pavilion Rise', '773431cd-f560-4be8-9e6f-b582a4ebf204', '0dd9476c-4058-4574-a35d-f846588b047c', 'PAVILION13', NULL, 'SS18 3PK', 'ca979718-b15d-4318-9944-69aaff281cad', 'temporary-accommodation', 'active', 0) ON CONFLICT (id) DO NOTHING;

INSERT INTO temporary_accommodation_premises (premises_id, probation_delivery_unit_id) VALUES
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

-- Premises for Approved Premises

insert into
  "premises" (
    "address_line1",
    "address_line2",
    "id",
    "latitude",
    "local_authority_area_id",
    "longitude",
    "name",
    "notes",
    "postcode",
    "probation_region_id",
    "service",
    "status",
    "total_beds",
    "town"
  )
values
  (
    '454 Nader Port',
    '',
    'ad7933a2-936f-423b-aa8f-bdee3465e461',
    51.904735,
    '87c21df8-c148-4bdd-9ba6-fb2b42097a1c',
    -1.1635,
    'Test AP 1',
    'No',
    'OX26 2EQ',
    'f6db2e41-040e-47c7-8bba-a345b6d35ca1',
    'approved-premises',
    'active',
    26,
    'Quitzonview'
  ),
  (
    '7525 Alba Trail',
    '',
    'f5f19ef8-5c83-48d3-863b-29796648cddd',
    53.266459,
    '2029e1a3-cb0b-45b8-b2cc-8224aaf94b6d',
    -2.768905,
    'Test AP 2',
    '',
    'WA6 9BQ',
    'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891',
    'approved-premises',
    'active',
    24,
    'Theronborough'
  ),
  (
    '8527 Bell Rapids',
    '',
    'ac0b0c5d-4185-4013-a249-3acddbb478f8',
    55.751711,
    '9b4e335f-d424-4828-a319-5812f0532263',
    -3.851103,
    'Test AP 3',
    'restrictions on offences against sex workers due to location of the AP',
    'ML8 5EP',
    'afee0696-8df3-4d9f-9d0c-268f17772e2c',
    'approved-premises',
    'active',
    25,
    'South Myah'
  ),
  (
    '306 Destinee Union',
    '',
    '755e6828-38de-41b9-b342-b25ff9f539cc',
    55.599949,
    '4387d59e-adea-4e63-b199-5c9a2a6af5ee',
    -4.376498,
    'Test AP 4',
    '',
    'KA4 8JG',
    '6b4a1308-17af-4c1a-a330-6005bec9e27b',
    'approved-premises',
    'active',
    23,
    'South Devinberg'
  ),
  (
    '4023 Green Wells',
    '',
    'f4bb600e-1132-4560-9f0b-919fdccda5b4',
    51.631685,
    '423774ac-ba2a-474c-a2ed-97f58c475a1a',
    -0.125257,
    'Test AP 5',
    'As an independent AP, cannot take Arsonists who post an imminent risk of further Arson. This is a local decision, I confirmed that there was no insurance issues regarding Arson. The manager just said if there was an Arsonist whose risk was seen as imminent, she would have to check with the Charity who funds the AP',
    'N14 6QW',
    '0544d95a-f6bb-43f8-9be7-aae66e3bf244',
    'approved-premises',
    'active',
    26,
    'Port Carleton'
  ),
  (
    '42716 Dickens Route',
    '',
    '0373b8e5-8c40-418c-84c7-875becc6ff6a',
    53.50205,
    '895d3ed6-01f6-4def-aa07-7006637a5f41',
    -2.790471,
    'Test AP 6',
    '',
    'WA11 8GJ',
    '6b4a1308-17af-4c1a-a330-6005bec9e27b',
    'approved-premises',
    'active',
    17,
    'Bayerstead'
  ),
  (
    '704 Eleazar Rue',
    '',
    '1702cdc6-464a-4532-88da-3204ae62943a',
    53.254466,
    'f2719521-7969-4ef3-9953-be2434ff40cd',
    -2.124048,
    'Test AP 7',
    '',
    'SK11 6TG',
    'afee0696-8df3-4d9f-9d0c-268f17772e2c',
    'approved-premises',
    'active',
    22,
    'Bergstromfort'
  ),
  (
    '069 Braun Flat',
    '',
    'e1eb5403-c72b-4ee2-b744-b72f7c30243f',
    50.913966,
    '2cd0b7a4-4948-40de-8e79-cfc12a13474a',
    -1.442888,
    'Test AP 8',
    '',
    'SO15 8PN',
    '5e44b880-df20-4751-938f-a14be5fe609d',
    'approved-premises',
    'active',
    22,
    'Roobbury'
  ),
  (
    '14063 Edyth Court',
    '',
    '31991429-ea1a-4c74-bd7c-cbc704792368',
    53.254737,
    '03e7d7a0-a203-47cc-8f35-13176adbf7e0',
    0.180603,
    'Test AP 9',
    'No',
    'LN13 9AT',
    'ca979718-b15d-4318-9944-69aaff281cad',
    'approved-premises',
    'active',
    21,
    'North Hallie'
  ),
  (
    '9317 Bauch Heights',
    '',
    '1f1cb812-a6d1-42dd-889f-7ea233b69f5e',
    50.886096,
    'badb12e9-e467-4b8d-9e0b-881c8455f5cf',
    0.424608,
    'Test AP 10',
    'No Child RSO''s since 2006 - National instruction. ',
    'TN33 9JN',
    '43606be0-9836-441d-9bc1-5586de9ac931',
    'approved-premises',
    'active',
    29,
    'Mavisworth'
  ),
  (
    '5510 Emmitt Street',
    '',
    'f93dbfcd-a5f6-46f5-9085-3caae62f6933',
    51.593159,
    '8ed5df89-6832-436c-bff6-355fdbfeb7a1',
    -3.781082,
    'Test AP 11',
    'Rooms 9 to 16 are currently out of use - annexe that contains them is subsiding and is non accesssible - for last 6 months or so.',
    'SA13 1NN',
    'afee0696-8df3-4d9f-9d0c-268f17772e2c',
    'approved-premises',
    'active',
    20,
    'North Gregoryboro'
  ),
  (
    '307 Annabell Flats',
    '',
    '63e89550-203f-4f89-bfbb-cd76bf4a5c06',
    51.119205,
    '995cf5b8-0635-43d7-86a1-e44a58f2a9bb',
    -1.040455,
    'Test AP 12',
    '',
    'GU34 5EB',
    'f6db2e41-040e-47c7-8bba-a345b6d35ca1',
    'approved-premises',
    'active',
    19,
    'Zemlaktown'
  ),
  (
    '110 Bailey Expressway',
    '',
    'd1fb0953-731f-4014-b182-9d793fb03500',
    51.249544,
    'cf5dd132-6c5a-430b-82f2-092966b46152',
    -0.637553,
    'Test AP 13',
    '',
    'GU3 3DZ',
    'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891',
    'approved-premises',
    'active',
    20,
    'Cordellburgh'
  ),
  (
    '0300 Blick Camp',
    '',
    '30515599-e6a2-4663-b88f-99c626428003',
    53.792705,
    '583fe416-c371-4352-b244-f66dfd1fb48c',
    -0.191504,
    'Test AP 14',
    'Westgate notes',
    'HU11 4XH',
    '734261a0-d053-4aed-968d-ffc518cc17f8',
    'approved-premises',
    'active',
    19,
    'North Moriahhaven'
  ),
  (
    '4169 Joana Branch',
    '',
    '36dd859b-fae4-4ab7-baf0-3c93bd7e7f9a',
    57.644221,
    'dc0cb492-dc19-4594-a166-30c009c5740e',
    -3.295377,
    'Test AP 15',
    'TEST notes',
    'IV30 1XX',
    'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891',
    'approved-premises',
    'active',
    19,
    'Alfredohaven'
  ) ON CONFLICT (id) DO NOTHING;

insert into
  "approved_premises" ("ap_code", "point", "premises_id", "q_code")
values
  (
    'TEST1',
    '0101000020E6100000F645425BCEF3494004560E2DB29DF2BF',
    'ad7933a2-936f-423b-aa8f-bdee3465e461',
    'Q124'
  ),
  (
    'TEST2',
    '0101000020E6100000C85C19541BA24A40D8D825AAB72606C0',
    'f5f19ef8-5c83-48d3-863b-29796648cddd',
    'Q224'
  ),
  (
    'TEST3',
    '0101000020E61000009085E81038E04B404C38F4160FCF0EC0',
    'ac0b0c5d-4185-4013-a249-3acddbb478f8',
    'Q324'
  ),
  (
    'TEST4',
    '0101000020E61000004B22FB20CBCC4B40A20914B1888111C0',
    '755e6828-38de-41b9-b342-b25ff9f539cc',
    'Q424'
  ),
  (
    'TEST5',
    '0101000020E6100000D72FD80DDBD04940522B4CDF6B08C0BF',
    'f4bb600e-1132-4560-9f0b-919fdccda5b4',
    'Q125'
  ),
  (
    'TEST6',
    '0101000020E6100000787AA52C43C04A40C87DAB75E25206C0',
    '0373b8e5-8c40-418c-84c7-875becc6ff6a',
    'Q225'
  ),
  (
    'TEST7',
    '0101000020E6100000D3F8855792A04A40DC12B9E00CFE00C0',
    '1702cdc6-464a-4532-88da-3204ae62943a',
    'Q325'
  ),
  (
    'TEST8',
    '0101000020E6100000F6D37FD6FC74494050A73CBA1116F7BF',
    'e1eb5403-c72b-4ee2-b744-b72f7c30243f',
    'Q425'
  ),
  (
    'TEST9',
    '0101000020E6100000630AD6389BA04A404E9D47C5FF1DC73F',
    '31991429-ea1a-4c74-bd7c-cbc704792368',
    'Q126'
  ),
  (
    'TEST10',
    '0101000020E6100000E78EFE976B7149408EAD6708C72CDB3F',
    '1f1cb812-a6d1-42dd-889f-7ea233b69f5e',
    'Q226'
  ),
  (
    'TEST11',
    '0101000020E6100000FE2955A2ECCB494045F46BEBA73F0EC0',
    'f93dbfcd-a5f6-46f5-9085-3caae62f6933',
    'Q326'
  ),
  (
    'TEST12',
    '0101000020E61000008542041C428F4940D95A5F24B4A5F0BF',
    '63e89550-203f-4f89-bfbb-cd76bf4a5c06',
    'Q426'
  ),
  (
    'TEST13',
    '0101000020E6100000DE74CB0EF19F49401CEF8E8CD566E4BF',
    'd1fb0953-731f-4014-b182-9d793fb03500',
    'Q127'
  ),
  (
    'TEST14',
    '0101000020E61000001630815B77E54A40BCCE86FC3383C8BF',
    '30515599-e6a2-4663-b88f-99c626428003',
    'Q227'
  ),
  (
    'TEST15',
    '0101000020E6100000BE326FD575D24C40BBECD79DEE5C0AC0',
    '36dd859b-fae4-4ab7-baf0-3c93bd7e7f9a',
    'Q327'
  ) ON CONFLICT (premises_id)
DO
  NOTHING;