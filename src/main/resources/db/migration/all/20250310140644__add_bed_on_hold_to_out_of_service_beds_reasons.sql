INSERT INTO cas1_out_of_service_bed_reasons(id, created_at, name, is_active)
SELECT '199bef2d-0839-40c0-85e2-00b84fde7fde',now(),'Bed on hold',true
WHERE NOT EXISTS ( SELECT id FROM cas1_out_of_service_bed_reasons WHERE id = '199bef2d-0839-40c0-85e2-00b84fde7fde');