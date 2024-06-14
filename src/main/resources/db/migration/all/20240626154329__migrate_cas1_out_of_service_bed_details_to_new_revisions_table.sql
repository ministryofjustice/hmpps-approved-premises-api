INSERT INTO cas1_out_of_service_bed_revisions
SELECT
    gen_random_uuid() AS id,
    id AS out_of_service_bed_id,
    out_of_service_bed_reason_id,
    NULL AS created_by_user_id,
    CURRENT_TIMESTAMP AS created_at,
    'INITIAL' AS revision_type,
    start_date,
    end_date,
    reference_number,
    notes
FROM cas1_out_of_service_beds;
