UPDATE users u
SET cas1_cru_management_area_id = (SELECT default_cru1_management_area_id FROM ap_areas area WHERE area.id = u.ap_area_id)
WHERE u.ap_area_id IS NOT NULL AND u.cas1_cru_management_area_id IS NULL;