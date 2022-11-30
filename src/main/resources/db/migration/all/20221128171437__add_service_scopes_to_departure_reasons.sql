ALTER TABLE departure_reasons ADD COLUMN service_scope TEXT;

UPDATE departure_reasons SET service_scope = 'approved-premises';

UPDATE departure_reasons
SET service_scope = '*'
WHERE id IN (
    '6589f8fb-d377-4bc4-ace9-d3a2ce02f97b', -- Left of own volition
    '5bb2a91b-a33a-425e-8503-2ba69d7cabe7', -- Admitted to Hospital
    '9a68687d-944b-4960-9de2-7645635910f3', -- Bed Withdrawn
    'c81c42fe-f7af-4d16-b67d-93f0b4cbd338', -- Breach / recall (abscond)
    'f5b57d36-1b3c-4777-93a2-72ff59cf7674', -- Breach / recall (behaviour / increasing risk)
    'b80cbd68-c223-40af-b82b-5f7cee6f6fd7', -- Breach / recall (curfew)
    'ff751e28-f957-4941-8049-5b44c6da878e', -- Breach / recall (house rules)
    'd6e3f1db-01a7-4ce4-b7cb-f1ff5454d62e', -- Breach / recall (licence or bail condition)
    'd86b6e2e-0b95-459a-a85a-065729149f0a', -- Breach / recall (other)
    '761cfbe1-c102-4fe9-b943-bf131d550c90', -- Breach / recall (positive drugs test)
    '69eac20f-c3b9-4ea3-87fd-814c3f1fd117', -- Died
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737', -- Planned move-on
    '60d030db-ce2d-4f1a-b24f-42ce15d268f7'  -- Other
);

ALTER TABLE departure_reasons ALTER COLUMN service_scope SET NOT NULL;

INSERT INTO departure_reasons (id, name, is_active, service_scope) VALUES
    ('ae93ec2c-157a-49fd-a4c9-b67f1a9457d8', 'Further custodial sentence imposed', true, 'temporary-accommodation');
