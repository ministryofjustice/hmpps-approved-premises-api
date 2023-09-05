ALTER TABLE assessment_referral_history_notes ADD COLUMN created_by_user_id UUID;

UPDATE assessment_referral_history_notes
SET created_by_user_id = n.created_by_user_id
FROM assessment_referral_history_user_notes n;

ALTER TABLE assessment_referral_history_notes ALTER COLUMN created_by_user_id SET NOT NULL;
ALTER TABLE assessment_referral_history_notes ADD FOREIGN KEY (created_by_user_id) REFERENCES users(id);

ALTER TABLE assessment_referral_history_user_notes DROP COLUMN created_by_user_id;

CREATE TABLE assessment_referral_history_system_notes (
    id UUID NOT NULL,
    type TEXT NOT NULL,
    FOREIGN KEY (id) REFERENCES assessment_referral_history_notes(id)
);
