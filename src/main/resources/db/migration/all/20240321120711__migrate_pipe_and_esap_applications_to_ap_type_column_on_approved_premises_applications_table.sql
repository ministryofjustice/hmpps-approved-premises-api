UPDATE approved_premises_applications
SET ap_type = 'PIPE'
WHERE is_pipe_application = true;

UPDATE approved_premises_applications
SET ap_type = 'ESAP'
WHERE is_esap_application = true;

ALTER TABLE approved_premises_applications ALTER COLUMN ap_type DROP DEFAULT;
