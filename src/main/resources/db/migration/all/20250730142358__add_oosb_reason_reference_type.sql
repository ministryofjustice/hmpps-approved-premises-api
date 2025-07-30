ALTER TABLE cas1_out_of_service_bed_reasons ADD reference_type varchar DEFAULT 'WORK_ORDER' NOT NULL;

UPDATE cas1_out_of_service_bed_reasons SET reference_type = 'CRN' WHERE name IN (
 'Double room with single occupancy – other (Non-FM)',
 'Double room with single occupancy – risk (Non-FM)',
 'Bed on hold'
);
