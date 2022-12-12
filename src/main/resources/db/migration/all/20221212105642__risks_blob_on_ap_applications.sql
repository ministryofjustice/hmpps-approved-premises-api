ALTER TABLE approved_premises_applications ADD COLUMN risk_ratings JSON;

UPDATE approved_premises_applications SET risk_ratings = '{"roshRisks": {"status": "NotFound"}, "mappa": {"status": "NotFound"}, "tier": {"status": "NotFound"}, "flags": {"status": "NotFound"}}';

ALTER TABLE approved_premises_applications ALTER COLUMN risk_ratings SET NOT NULL;
