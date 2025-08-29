
UPDATE referral_rejection_reasons
SET name = 'No recourse to public funds (NRPF)' 
WHERE name = 'They have no recourse to public funds (NRPF)' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons
SET name = 'Not eligible (e.g. already released into the community, HDC)' 
WHERE name = 'They''re not eligible (not because of NRPF)' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons 
SET name = 'Not enough time to place' 
WHERE name = 'There was not enough time to place them' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons 
SET name = 'Not enough time on their licence or post-sentence supervision (PSS)' 
WHERE name = 'There was not enough time on their licence or post-sentence supervision (PSS)' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons 
SET name = 'Risk or needs cannot be safely managed in CAS3' 
WHERE name = 'Their risk or needs cannot be safely managed in CAS3' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons 
SET name = 'No bedspace available in PDU' 
WHERE name = 'The PDU has no available bedspaces at all' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons 
SET name = 'Single occupancy bedspace not available' 
WHERE name = 'No single occupancy bedspace is available' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons 
SET name = 'Suitable bedspace not available (not related to single occupancy availability)' 
WHERE name = 'No suitable bedspace is available (not because of single occupancy)' AND service_scope = 'temporary-accommodation';

UPDATE referral_rejection_reasons 
SET name = 'Supplier unable to accommodate (e.g. arson needs cannot be met)' 
WHERE name = 'Another reason (please add)' AND service_scope = 'temporary-accommodation';

INSERT INTO referral_rejection_reasons(id, name, is_active, service_scope, sort_order) VALUES
    ('f47ac10b-58cc-4372-a567-0e02b2c3d470', 'CAS1/AP alternative suitable accommodation provided', true, 'temporary-accommodation', 10),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d471', 'CAS2 alternative accommodation provided', true, 'temporary-accommodation', 11),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d472', 'Consent not given by person on probation', true, 'temporary-accommodation', 12),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d473', 'Local authority alternative suitable accommodation provided (includes Priority Need)', true, 'temporary-accommodation', 13),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d474', 'Other alternative accommodation provided (e.g. friends or family)', true, 'temporary-accommodation', 14),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d475', 'Out of region referral', true, 'temporary-accommodation', 15),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d476', 'Person has had maximum CAS3 placements on current sentence', true, 'temporary-accommodation', 16),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d477', 'Previous behavioural concerns in CAS3', true, 'temporary-accommodation', 17),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d478', 'Referral submitted too early', true, 'temporary-accommodation', 18),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'Remanded in custody or detained', true, 'temporary-accommodation', 19);