ALTER TABLE approved_premises ADD COLUMN ap_code TEXT;

UPDATE approved_premises ap SET ap_code = p.ap_code
    FROM premises p
WHERE p.id = ap.premises_id;

INSERT INTO approved_premises (premises_id, q_code, ap_code)
    SELECT p.id, 'QCODE', p.ap_code FROM premises p
    WHERE (SELECT COUNT(1) FROM approved_premises WHERE premises_id = p.id) = 0;

ALTER TABLE approved_premises ALTER COLUMN ap_code SET NOT NULL;
ALTER TABLE premises DROP COLUMN ap_code;
