-- add parent reason id to departure reason unique constraint
ALTER TABLE departure_reasons DROP CONSTRAINT departure_reason_active_name_service_scope_unique;
ALTER TABLE departure_reasons ADD CONSTRAINT departure_reason_unique UNIQUE ("name",is_active,service_scope,parent_reason_id);

-- convert 'other' wild-carded reason to be CAS3 only
UPDATE departure_reasons SET service_scope='temporary-accommodation' WHERE id='60d030db-ce2d-4f1a-b24f-42ce15d268f7'::uuid;

-- add cas1 specific 'other' reason
INSERT INTO departure_reasons (id,"name",is_active,service_scope,legacy_delius_reason_code)
VALUES ('1ecc1a09-725f-44d8-b03b-314d1ff4da62','Other',true,'approved-premises','W');