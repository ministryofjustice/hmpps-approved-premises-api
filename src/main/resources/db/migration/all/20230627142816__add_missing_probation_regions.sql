CREATE TABLE probation_area_probation_region_mappings (
    id UUID NOT NULL,
    probation_area_delius_code TEXT NOT NULL,
    probation_region_id UUID NOT NULL,
    PRIMARY KEY (id),
    UNIQUE(probation_area_delius_code),
    FOREIGN KEY (probation_region_id) REFERENCES probation_regions(id)
);

--Existing mappings from the delius_code column on probation_regions that we are replacing
INSERT INTO probation_area_probation_region_mappings (id, probation_area_delius_code, probation_region_id) VALUES
    ('96724d1a-a35d-44f0-b28a-c0362b393971', 'N53', '0544d95a-f6bb-43f8-9be7-aae66e3bf244'),
    ('1e636503-ee4a-4853-a1f1-cff7cc3cbc5a', 'N56', 'ca979718-b15d-4318-9944-69aaff281cad'),
    ('292001f5-db2c-456f-84b4-bf9007ad21b0', 'MCG', 'f6db2e41-040e-47c7-8bba-a345b6d35ca1'),
    ('e1534d5a-fc7b-4dc0-a1c6-093b6c8467e7', 'N57', 'db82d408-d440-4eb5-960b-119cb33427cd'),
    ('c5eb4f68-0220-456b-9466-f0da2be19d1b', 'N07', 'd73ae6b5-041e-4d44-b859-b8c77567d893'),
    ('4ece1e3f-76a1-401a-9fc0-058ecebb4466', 'N54', 'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891'),
    ('ab05d3ab-3505-4679-8237-75365f82984d', 'N51', 'a02b7727-63aa-46f2-80f1-e0b05b31903c'),
    ('0d1f8111-3230-4af1-8ed2-9c3b4949382b', 'N59', '6b4a1308-17af-4c1a-a330-6005bec9e27b'),
    ('5605a8a1-6e75-4621-b043-4704949a18b6', 'N58', '43606be0-9836-441d-9bc1-5586de9ac931'),
    ('8a2037f1-922f-4358-afe9-f51cef5d2102', 'N03', 'afee0696-8df3-4d9f-9d0c-268f17772e2c'),
    ('fa885b2b-b5b2-41f4-ad69-5c4fa76a3ea1', 'N52', '734261a0-d053-4aed-968d-ffc518cc17f8'),
    ('0d5db4e0-6e4b-41f3-88af-b7a649b6c802', 'N55', '5e44b880-df20-4751-938f-a14be5fe609d');

-- New mappings to existing Probation Regions

INSERT INTO probation_area_probation_region_mappings (id, probation_area_delius_code, probation_region_id) VALUES
    ('4b8fc10a-9d7c-4faa-ae5b-b2e8049936aa', 'N02', 'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891'), -- `NPS North East` to `North East`
    ('ff8a7012-0202-4f76-a6a5-a500696d58fd', 'N06', 'db82d408-d440-4eb5-960b-119cb33427cd'), -- `NPS South East and Eastern` to Kent, `Surrey & Sussex`
    ('4dd875fd-decf-4b9b-b19e-619d76eaba97', 'N32', 'c5acff6c-d0d2-4b89-9f4d-89a15cfa3891'), -- `Ext - North East Region` to `North East`
    ('75426a6c-d422-411d-b89f-c99c6b5fd482', 'N33', '5e44b880-df20-4751-938f-a14be5fe609d'); -- `Ext - Yorkshire and The Humber` to `Yorkshire & The Humber`

-- National ApArea/Probation Region to represent Delius Probation Areas that are not geographical
INSERT INTO ap_areas (id, identifier, "name") VALUES
    ('2fa23a25-610c-4728-aea6-c84e0207120b', 'National', 'NAT');

INSERT INTO probation_regions (id, name, ap_area_id, delius_code) VALUES
    ('dafc93d2-2dec-42d1-84d9-190e9ccda638', 'National', '2fa23a25-610c-4728-aea6-c84e0207120b', 'NA');

INSERT INTO probation_area_probation_region_mappings (id, probation_area_delius_code, probation_region_id) VALUES
    ('7edab871-1945-4117-8c49-811a5ed66a7d', 'N41', 'dafc93d2-2dec-42d1-84d9-190e9ccda638'), -- `National Responsibility Divison`
    ('45ab458b-46c8-4460-b2cb-fcf953c20650', 'N43', 'dafc93d2-2dec-42d1-84d9-190e9ccda638'), -- `National Security Division`
    ('7fe99bf9-0f36-478f-bb89-4d82eec62884', 'XXX', 'dafc93d2-2dec-42d1-84d9-190e9ccda638'); -- `ZZ BAST Public Provider 1`
