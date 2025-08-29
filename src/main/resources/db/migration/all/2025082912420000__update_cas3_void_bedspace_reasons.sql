
UPDATE cas3_void_bedspace_reasons
SET name = 'Cooling off after incident (e.g. crime scene)'
WHERE name = 'Cooling off (e.g. following an incident)';

UPDATE cas3_void_bedspace_reasons
SET name = 'Damage repairs (e.g. by person, from flooding)'
WHERE name = 'PoP damage/repairs';

UPDATE cas3_void_bedspace_reasons
SET name = 'Replacing furniture or stolen goods'
WHERE name = 'Stolen goods/furniture replacement';

UPDATE cas3_void_bedspace_reasons
SET name = 'Maintenance repairs (e.g. damp, infestation)'
WHERE name = 'Supplier maintenance/repairs';

UPDATE cas3_void_bedspace_reasons
SET is_active = false
WHERE name = 'Other';

INSERT INTO cas3_void_bedspace_reasons(id, name, is_active) VALUES
    ('f47ac10b-58cc-4372-a567-0e02b2c3d480', 'Occupancy reduced (e.g. from 2 bed to single)', true),
    ('6ba7b818-9dad-11d1-80b4-00c04fd430c8', 'Pending handback of property', true),
    ('ee5b1638-d578-4fc6-b83f-1c9b94b5bffd', 'Death – personal property collection', true);
