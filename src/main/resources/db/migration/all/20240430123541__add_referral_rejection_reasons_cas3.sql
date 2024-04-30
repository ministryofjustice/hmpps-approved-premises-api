UPDATE referral_rejection_reasons
SET name = 'The PDU has no available bedspaces at all'
WHERE id = '155ee6dc-ac2a-40d2-a350-90b63fb34a06';

UPDATE referral_rejection_reasons
SET name = 'Another reason (please add)'
WHERE id= '85799bf8-8b64-4903-9ab8-b08a77f1a9d3';

INSERT INTO referral_rejection_reasons(id,name,is_active,service_scope) VALUES
    ('b19ba749-408f-48c0-907c-11eace2dcf67','No single occupancy bedspace is available',true,'temporary-accommodation'),
    ('311de468-078b-4c39-ae42-8d41575b7726','No suitable bedspace is available (not because of single occupancy)',true,'temporary-accommodation');