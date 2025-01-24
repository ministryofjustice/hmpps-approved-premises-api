-- previously 'Parole Licence not granted'
UPDATE non_arrival_reasons SET "name"='Parole release not granted' WHERE id='4e27ab56-72c1-47c8-b3e2-05d353b0650e'::uuid;
-- Custodial disposal - RIC
UPDATE non_arrival_reasons SET is_active=false WHERE id='9d3b1f8e-9fa6-45b7-84ac-2d5fe34ff935'::uuid;
-- previously 'Other'
UPDATE non_arrival_reasons SET "name"='Other - provide reasons' WHERE id='3635c76e-8e4b-4c0e-8b92-149dc1ff0855'::uuid;
-- Withdrawn by Referrer
UPDATE non_arrival_reasons SET is_active=false WHERE id='91259165-f6f5-4ece-b822-fdbef5fcb668'::uuid;
-- a97f2010-ef70-4c65-97a0-648d18ebc71b
UPDATE non_arrival_reasons SET is_active=false WHERE id='a97f2010-ef70-4c65-97a0-648d18ebc71b'::uuid;
-- previously 'Failed to Arrive'
UPDATE non_arrival_reasons SET "name"='Failed to arrive as planned' WHERE id='e9184f2e-f409-461e-b149-492a02cb1655'::uuid;
INSERT INTO non_arrival_reasons (id,"name",is_active,legacy_delius_reason_code) VALUES ('6d86a963-f950-4b4b-9bc7-ae42f6071abb','Admitted to hospital',true,'TBD');
INSERT INTO non_arrival_reasons (id,"name",is_active,legacy_delius_reason_code) VALUES ('3892cbde-29a3-4b90-9c72-1b769822a31d','Arrested / In custody / Recalled',true,'TBD');