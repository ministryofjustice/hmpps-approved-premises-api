UPDATE non_arrival_reasons SET legacy_delius_reason_code = '1H' WHERE name = 'Arrested / In custody / Recalled';
UPDATE non_arrival_reasons SET legacy_delius_reason_code = 'H' WHERE name = 'Admitted to hospital';

UPDATE move_on_categories SET legacy_delius_category_code = 'CAS2' WHERE service_scope = 'approved-premises' AND name = 'CAS2';
UPDATE move_on_categories SET legacy_delius_category_code = 'CAS3' WHERE service_scope = 'approved-premises' AND name = 'CAS3';

UPDATE departure_reasons SET legacy_delius_reason_code = 'PAT' WHERE service_scope = 'approved-premises' AND name = 'Positive alcohol test / use';

DELETE from departure_reasons WHERE service_scope = 'approved-premises' AND name = 'Behaviour / increasing risk in AP';
UPDATE departure_reasons SET name = 'Behaviour / increasing risk in AP' WHERE  service_scope = 'approved-premises' AND name = 'Behaviour / increasing risk';