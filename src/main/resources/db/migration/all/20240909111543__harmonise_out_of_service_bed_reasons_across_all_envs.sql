
INSERT INTO cas1_out_of_service_bed_reasons(id, created_at, name, is_active)
SELECT '2f46e769-17a5-4b5c-b04a-a5a9a5b3f773',now(),'Planned Refurbishment',true
WHERE NOT EXISTS ( SELECT id FROM cas1_out_of_service_bed_reasons WHERE id = '2f46e769-17a5-4b5c-b04a-a5a9a5b3f773');

INSERT INTO cas1_out_of_service_bed_reasons(id, created_at, name, is_active)
SELECT '34467c03-b919-4423-b4e2-e0dc92520a56',now(),'Damage by Resident',true
WHERE NOT EXISTS ( SELECT id FROM cas1_out_of_service_bed_reasons WHERE id = '34467c03-b919-4423-b4e2-e0dc92520a56');

INSERT INTO cas1_out_of_service_bed_reasons(id, created_at, name, is_active)
SELECT '2e947d1a-c547-4a09-9b76-76a609771307',now(),'Accident/Flood/Fire',true
WHERE NOT EXISTS ( SELECT id FROM cas1_out_of_service_bed_reasons WHERE id = '2e947d1a-c547-4a09-9b76-76a609771307');

INSERT INTO cas1_out_of_service_bed_reasons(id, created_at, name, is_active)
SELECT 'abd5af8d-8f8c-469a-8e22-85635f99ec0d',now(),'Staff Shortage/Illness',true
WHERE NOT EXISTS ( SELECT id FROM cas1_out_of_service_bed_reasons WHERE id = 'abd5af8d-8f8c-469a-8e22-85635f99ec0d');

INSERT INTO cas1_out_of_service_bed_reasons(id, created_at, name, is_active)
SELECT 'ce0c151c-dda5-450c-8a7f-ca8895fecd04',now(),'Double Room with Single occupancy - risk',true
WHERE NOT EXISTS ( SELECT id FROM cas1_out_of_service_bed_reasons WHERE id = 'ce0c151c-dda5-450c-8a7f-ca8895fecd04');

INSERT INTO cas1_out_of_service_bed_reasons(id, created_at, name, is_active)
SELECT '55594aa8-1ae1-4a3c-b6f3-7bb55ff14807',now(),'Double room with single occupancy - health',true
WHERE NOT EXISTS ( SELECT id FROM cas1_out_of_service_bed_reasons WHERE id = '55594aa8-1ae1-4a3c-b6f3-7bb55ff14807');

INSERT INTO cas1_out_of_service_bed_reasons(id, created_at, name, is_active)
SELECT '6943e9e7-ffae-44ba-b2f4-bc299af8773c',now(),'Planned FM works required',true
WHERE NOT EXISTS ( SELECT id FROM cas1_out_of_service_bed_reasons WHERE id = '6943e9e7-ffae-44ba-b2f4-bc299af8773c');
