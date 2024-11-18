ALTER TABLE departure_reasons ADD parent_reason_id UUID NULL;
ALTER TABLE departure_reasons ADD CONSTRAINT parent_reason_fk FOREIGN KEY (parent_reason_id) REFERENCES departure_reasons (id);

INSERT INTO departure_reasons(id, name, is_active, service_scope, legacy_delius_reason_code, parent_reason_id)
SELECT 'd3e43ec3-02f4-4b96-a464-69dc74099259', 'Breach / Recall', TRUE, 'approved-premises', NULL, NULL
WHERE NOT EXISTS (
    SELECT id FROM departure_reasons where id = 'd3e43ec3-02f4-4b96-a464-69dc74099259'
);

UPDATE departure_reasons
    SET
        name = upper(substring(SUBSTRING(name, '\((.+)\)') from 1 for 1)) || substring(SUBSTRING(name, '\((.+)\)') from 2),
        parent_reason_id = 'd3e43ec3-02f4-4b96-a464-69dc74099259'
WHERE
    NAME LIKE 'Breach / recall %' AND
    service_scope = 'approved-premises';

