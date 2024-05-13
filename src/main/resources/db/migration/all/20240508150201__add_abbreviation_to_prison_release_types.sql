Alter Table prison_release_types ADD COLUMN abbreviation TEXT;

UPDATE prison_release_types
SET abbreviation = 'PSS'
WHERE id='740f6dd5-1ec2-493b-893e-ca2406464e36';

UPDATE prison_release_types
SET abbreviation = 'ECSL'
WHERE id='f750a14e-a815-49d8-a70d-901734c16f27';

UPDATE prison_release_types
SET abbreviation = 'Parole'
WHERE id='d8fa5a15-bddf-4f51-ac0f-4c8f7ed5dfd8';

UPDATE prison_release_types
SET abbreviation = 'Fixed-term recall'
WHERE id='9ffa8a3a-bd8a-472b-9302-3e97c433eec7';

UPDATE prison_release_types
SET abbreviation = 'Standard recall'
WHERE id='9b887b63-02f0-4441-b609-abf5f44fd261';

UPDATE prison_release_types
SET abbreviation = 'CRD licence'
WHERE id='82ce603b-a7d1-4e65-bab5-ed750917840a';