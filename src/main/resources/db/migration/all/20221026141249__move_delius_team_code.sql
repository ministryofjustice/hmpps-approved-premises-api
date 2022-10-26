ALTER TABLE approved_premises ADD COLUMN delius_team_code TEXT;

UPDATE approved_premises ap SET delius_team_code = p.delius_team_code
    FROM premises p
WHERE p.id = ap.premises_id;

ALTER TABLE approved_premises ALTER COLUMN delius_team_code SET NOT NULL;
ALTER TABLE premises DROP COLUMN delius_team_code;
