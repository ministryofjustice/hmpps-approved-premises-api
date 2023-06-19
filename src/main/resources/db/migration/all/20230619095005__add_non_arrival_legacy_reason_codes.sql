ALTER TABLE non_arrival_reasons ADD COLUMN legacy_delius_reason_code TEXT;

-- Failed to Arrive
UPDATE non_arrival_reasons SET legacy_delius_reason_code = 'A' WHERE id = 'e9184f2e-f409-461e-b149-492a02cb1655';

-- Offer Withdrawn
UPDATE non_arrival_reasons SET legacy_delius_reason_code = 'B' WHERE id = 'a97f2010-ef70-4c65-97a0-648d18ebc71b';

-- Withdrawn by Referrer
UPDATE non_arrival_reasons SET legacy_delius_reason_code = 'C' WHERE id = '91259165-f6f5-4ece-b822-fdbef5fcb668';

-- Other
UPDATE non_arrival_reasons SET legacy_delius_reason_code = 'D' WHERE id = '3635c76e-8e4b-4c0e-8b92-149dc1ff0855';

-- Custodial disposal - RIC
UPDATE non_arrival_reasons SET legacy_delius_reason_code = '1H' WHERE id = '9d3b1f8e-9fa6-45b7-84ac-2d5fe34ff935';

-- Parole Licence not granted
UPDATE non_arrival_reasons SET legacy_delius_reason_code = '4I' WHERE id = '4e27ab56-72c1-47c8-b3e2-05d353b0650e';
