-- Supports Cas2ApplicationSummaryRepository queries filtering by service origin and the creating user
CREATE INDEX cas_2_applications_service_origin_created_by_cas2_user_id_idx ON cas_2_applications (service_origin, created_by_cas2_user_id);

-- Support the 'crnOrNomsNumber' search in findCas2ApplicationSummaries (a.crn = :x OR a.noms_number = :x),
-- allowing a bitmap OR of the two indexes instead of a sequential scan
CREATE INDEX cas_2_applications_crn_idx ON cas_2_applications (crn);
CREATE INDEX cas_2_applications_noms_number_idx ON cas_2_applications (noms_number);
