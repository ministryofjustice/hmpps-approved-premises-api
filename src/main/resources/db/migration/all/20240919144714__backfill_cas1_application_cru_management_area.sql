UPDATE approved_premises_applications apa
SET cas1_cru_management_area_id = (SELECT default_cru1_management_area_id FROM ap_areas area WHERE area.id = apa.ap_area_id)
WHERE apa.ap_area_id IS NOT NULL AND apa.cas1_cru_management_area_id IS NULL;