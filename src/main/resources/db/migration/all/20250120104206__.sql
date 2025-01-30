-- Was 'Double room with single occupancy - health'
UPDATE public.cas1_out_of_service_bed_reasons SET "name"='Double room with single occupancy – other (Non-FM)'
WHERE id='55594aa8-1ae1-4a3c-b6f3-7bb55ff14807'::uuid;

-- Was 'Double Room with Single occupancy - risk'
UPDATE public.cas1_out_of_service_bed_reasons SET "name"='Double room with single occupancy – risk (Non-FM)'
WHERE id='ce0c151c-dda5-450c-8a7f-ca8895fecd04'::uuid;

-- Was 'Staff Shortage/Illness'
UPDATE public.cas1_out_of_service_bed_reasons SET "name"='Staff shortage / Illness (Non-FM)'
WHERE id='abd5af8d-8f8c-469a-8e22-85635f99ec0d'::uuid;

-- Was 'Damage by Resident'
UPDATE public.cas1_out_of_service_bed_reasons SET "name"='Damage by resident requiring FM Repair'
WHERE id='34467c03-b919-4423-b4e2-e0dc92520a56'::uuid;

-- Was 'Planned Refurbishment'
UPDATE public.cas1_out_of_service_bed_reasons SET "name"='Planned Refurbishment (FM)'
WHERE id='2f46e769-17a5-4b5c-b04a-a5a9a5b3f773'::uuid;

-- Accident/Flood/Fire
UPDATE public.cas1_out_of_service_bed_reasons SET is_active=false
WHERE id='2e947d1a-c547-4a09-9b76-76a609771307'::uuid;

INSERT INTO public.cas1_out_of_service_bed_reasons (id,created_at,"name",is_active)
VALUES ('2f076b35-72d2-410e-9a92-6d00781a559c','NOW()','Other unplanned FM works required',true);
INSERT INTO public.cas1_out_of_service_bed_reasons (id,created_at,"name",is_active)
VALUES ('56da56bd-f20f-4830-9e44-ed3b7795cf5e','NOW()','Flood / Fire requiring FM repair',true);
INSERT INTO public.cas1_out_of_service_bed_reasons (id,created_at,"name",is_active)
VALUES ('051e7f55-a5a5-47a9-9bbf-a7b11a6ebf97','NOW()','Incident not requiring FM repair',true);
INSERT INTO public.cas1_out_of_service_bed_reasons (id,created_at,"name",is_active)
VALUES ('0e2c5f8b-bee7-4947-a3aa-a09d09af611e','NOW()','AP Closure / Operational Reduction in Capacity (Non-FM)',true);