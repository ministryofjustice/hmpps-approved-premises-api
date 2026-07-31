-- CAS3 characteristics now live in cas3_premises_characteristics / cas3_bedspace_characteristics,
-- so the shared characteristics table is CAS1-only. Remove the temporary-accommodation rows and
-- the now-redundant service_scope column.
DELETE FROM characteristics WHERE service_scope = 'temporary-accommodation';

ALTER TABLE characteristics
DROP CONSTRAINT characteristics_property_name_service_scope_model_scope_key;

ALTER TABLE characteristics
DROP COLUMN service_scope;

ALTER TABLE characteristics
    ADD CONSTRAINT characteristics_property_name_model_scope_key UNIQUE (property_name, model_scope);
