ALTER TABLE temporary_accommodation_assessments
    ADD COLUMN referral_rejection_reason_id UUID;
ALTER TABLE temporary_accommodation_assessments
    ADD FOREIGN KEY (referral_rejection_reason_id) REFERENCES referral_rejection_reasons(id);