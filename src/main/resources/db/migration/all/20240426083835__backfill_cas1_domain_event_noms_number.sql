UPDATE domain_events d
SET noms_number = (
    SELECT noms_number from applications a where a.id = d.application_id
    )
WHERE d.service = 'CAS1' and d.noms_number IS NULL;