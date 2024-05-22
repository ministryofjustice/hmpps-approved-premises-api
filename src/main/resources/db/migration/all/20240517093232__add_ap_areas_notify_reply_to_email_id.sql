ALTER TABLE ap_areas ADD COLUMN notify_reply_to_email_id text NULL;

UPDATE ap_areas SET notify_reply_to_email_id = 'c6c686f8-fcb1-4c95-9a86-edee1d926e19' WHERE identifier = 'LON';
UPDATE ap_areas SET notify_reply_to_email_id = '80dc1290-ac8b-4121-aa79-ea950ad790df' WHERE identifier = 'Mids';
UPDATE ap_areas SET notify_reply_to_email_id = 'eb7e080c-d463-45f4-acfc-c0e74d964508' WHERE identifier = 'NE';
UPDATE ap_areas SET notify_reply_to_email_id = '89cd0f2b-7e7b-41e7-b868-5c98317843ec' WHERE identifier = 'NW';
UPDATE ap_areas SET notify_reply_to_email_id = '841578d9-a24d-4f63-8ced-1108785e4cbe' WHERE identifier = 'SEE';
UPDATE ap_areas SET notify_reply_to_email_id = '199c02f0-a08d-488e-9763-6bc8ffab3daf' WHERE identifier = 'SWSC';
UPDATE ap_areas SET notify_reply_to_email_id = '21a9314b-d26d-466a-a6e0-4ae85dbfee7b' WHERE identifier = 'Wales';

-- this uses the default reply to email id for CAS1 as NAT is deprecated and will soon be removed
UPDATE ap_areas SET notify_reply_to_email_id = '29226e7d-cdf7-44f2-b5c5-38de6ecf8df1' WHERE identifier = 'National';

ALTER TABLE ap_areas ALTER COLUMN notify_reply_to_email_id SET NOT NULL;
