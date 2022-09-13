ALTER TABLE cancellations DROP COLUMN reason;
ALTER TABLE cancellations ADD COLUMN cancellation_reason_id UUID NOT NULL;
ALTER TABLE cancellations ADD CONSTRAINT cancellation_reason_id_fk FOREIGN KEY (cancellation_reason_id) REFERENCES cancellation_reasons(id);
