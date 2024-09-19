ALTER TABLE approved_premises ALTER COLUMN gender SET DEFAULT 'MAN'::text;
UPDATE approved_premises SET gender = 'MAN';