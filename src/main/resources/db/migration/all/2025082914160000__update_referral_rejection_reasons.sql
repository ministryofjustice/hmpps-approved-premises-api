
UPDATE referral_rejection_reasons
SET is_active = false
WHERE name = 'Another reason (please add)' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons
SET name = 'Suitable bedspace not available (not related to single occupancy availability)', sort_order = 18
WHERE name = 'No suitable bedspace is available (not because of single occupancy)' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons
SET name = 'Single occupancy bedspace not available', sort_order = 17
WHERE name = 'No single occupancy bedspace is available' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons
SET name = 'No bedspace available in PDU', sort_order = 5
WHERE name = 'The PDU has no available bedspaces at all' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons
SET name = 'Risk or needs cannot be safely managed in CAS3', sort_order = 16
WHERE name = 'Their risk or needs cannot be safely managed in CAS3' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons
SET name = 'Not enough time on their licence or post-sentence supervision (PSS)', sort_order = 8
WHERE name = 'There was not enough time on their licence or post-sentence supervision (PSS)' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons
SET name = 'Not enough time to place', sort_order = 9
WHERE name = 'There was not enough time to place them' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons
SET name = 'No recourse to public funds (NRPF)', sort_order = 6
WHERE name = 'They have no recourse to public funds (NRPF)' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons
SET is_active = false
WHERE name = 'They’re not eligible (not because of NRPF)' AND service_scope = 'temporary-accommodation';

INSERT INTO referral_rejection_reasons(id, name, is_active, service_scope, sort_order) VALUES
    ('f47ac10b-58cc-4372-a567-0e02b2c3d470', 'CAS1/AP alternative suitable accommodation provided', true, 'temporary-accommodation', 1),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d471', 'CAS2 alternative accommodation provided', true, 'temporary-accommodation', 2),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d472', 'Consent not given by person on probation', true, 'temporary-accommodation', 3),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d473', 'Local authority alternative suitable accommodation provided (includes Priority Need)', true, 'temporary-accommodation', 4),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d474', 'Not eligible (e.g. already released into the community, HDC)', true, 'temporary-accommodation', 7),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d475', 'Other alternative accommodation provided (e.g. friends or family)', true, 'temporary-accommodation', 10),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d476', 'Out of region referral', true, 'temporary-accommodation', 11),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d477', 'Person has had maximum CAS3 placements on current sentence', true, 'temporary-accommodation', 12),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d478', 'Previous behavioural concerns in CAS3', true, 'temporary-accommodation', 13),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'Referral submitted too early', true, 'temporary-accommodation', 14),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d480', 'Remanded in custody or detained', true, 'temporary-accommodation', 15),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d481', 'Supplier unable to accommodate (e.g. arson needs cannot be met)', true, 'temporary-accommodation', 19);
