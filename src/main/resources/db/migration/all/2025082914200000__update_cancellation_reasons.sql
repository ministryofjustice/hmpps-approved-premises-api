
UPDATE cancellation_reasons 
SET name = 'Administrative error (e.g. a data entry error)' 
WHERE name = 'Administrative error' AND service_scope = 'temporary-accommodation';

UPDATE cancellation_reasons 
SET name = 'Local authority alternative suitable accommodation provided' 
WHERE name = 'Alternative suitable accommodation provided by Local Authority' AND service_scope = 'temporary-accommodation';

UPDATE cancellation_reasons 
SET name = 'Other alternative accommodation provided (e.g. friends or family)' 
WHERE name = 'Alternative suitable accommodation provided - Other (inc. AP, CAS2, friends/family etc.)' AND service_scope = 'temporary-accommodation';

UPDATE cancellation_reasons 
SET name = 'Person on probation failed to arrive (other reason)' 
WHERE name = 'Person on probation failed to arrive' AND service_scope = 'temporary-accommodation';

UPDATE cancellation_reasons 
SET name = 'Person on probation failed to arrive (gate arrest/recall)' 
WHERE name = 'Withdrawn by referrer (e.g. recalled, further custody, placement related risk concern)' AND service_scope = 'temporary-accommodation';

UPDATE cancellation_reasons 
SET name = 'Supplier unable to accommodate (explain)' 
WHERE name = 'Supplier unable to accommodate (Please explain in notes)' AND service_scope = 'temporary-accommodation';

INSERT INTO cancellation_reasons(id, name, is_active, service_scope, sort_order) VALUES
    ('f47ac10b-58cc-4372-a567-0e02b2c3d480', 'CAS1/AP alternative suitable accommodation provided', true, 'temporary-accommodation', 8),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d481', 'CAS2 alternative accommodation provided', true, 'temporary-accommodation', 9),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d482', 'Changes to booking - new booking not required', true, 'temporary-accommodation', 10),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d483', 'No single occupancy bedspace is available', true, 'temporary-accommodation', 11),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d484', 'Person on probation failed to arrive (alternative accommodation)', true, 'temporary-accommodation', 12),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d485', 'Risk or needs cannot be safely managed in CAS3 – new booking required', true, 'temporary-accommodation', 13),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d486', 'Risk or needs cannot be safely managed in CAS3 – new booking not required', true, 'temporary-accommodation', 14),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d487', 'Changes to booking - new booking required (e.g. new release date)', true, 'temporary-accommodation', 15);