DELETE FROM lost_beds WHERE premises_id IN (SELECT id FROM premises WHERE service = 'approved-premises');
DELETE FROM approved_premises;
DELETE FROM premises WHERE service = 'approved-premises';

INSERT INTO premises (id, "name", postcode, total_beds, probation_region_id, local_authority_area_id, service, notes, address_line1) VALUES
    ('459eeaba-55ac-4a1f-bae2-bad810d4016b', 'Beckenham Road', 'GL56 0QQ', 20, 'd73ae6b5-041e-4d44-b859-b8c77567d893', '5283aca7-21bd-4ded-96ec-09cc6f76468e', 'approved-premises', 'Notes', '1 Somewhere'),
    ('e03c82e9-f335-414a-87a0-866060397d4a', 'Bedford AP', 'GL56 0QQ', 30, '0544d95a-f6bb-43f8-9be7-aae66e3bf244', '7de4177b-9177-4c28-9bb6-5f5292619546', 'approved-premises', 'Notes', '2 Somewhere');

INSERT INTO approved_premises (premises_id, q_code, ap_code) VALUES
    ('459eeaba-55ac-4a1f-bae2-bad810d4016b', 'Q022', 'BCKNHAM'),
    ('e03c82e9-f335-414a-87a0-866060397d4a', 'Q005', 'BDFORD');
