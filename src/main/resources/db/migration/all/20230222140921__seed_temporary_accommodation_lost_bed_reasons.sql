INSERT INTO lost_bed_reasons (id, "name", is_active, service_scope)
VALUES
    ('040dce03-24f8-4fc4-8536-c1a311a307b3', 'Deep clean', true, 'temporary-accommodation'),
    ('1cfd451c-afde-4dba-a157-c1cf3289299f', 'Needle sweep', true, 'temporary-accommodation'),
    ('6697bb59-5b2b-48c7-82b1-d73013018602', 'PoP damage/repairs', true, 'temporary-accommodation'),
    ('ae8cfadf-b556-4119-a471-baa87d316ef9', 'Supplier maintenance/repairs', true, 'temporary-accommodation'),
    ('479e8765-a978-4343-9eba-34f5e055bd30', 'Stolen goods/furniture replacement', true, 'temporary-accommodation'),
    ('607c5526-e101-4b8b-b26d-dfefb4173e1d', 'Cooling off (e.g. following an incident)', true, 'temporary-accommodation');

UPDATE lost_bed_reasons
SET service_scope = '*'
WHERE "name" = 'Other';
