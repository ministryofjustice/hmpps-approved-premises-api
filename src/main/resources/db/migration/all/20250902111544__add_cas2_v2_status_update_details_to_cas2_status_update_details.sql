INSERT INTO cas_2_status_update_details as sd2
SELECT
    sd2v2.id,
    sd2v2.status_update_id,
    sd2v2.status_detail_id,
    sd2v2.label,
    sd2v2.created_at
FROM cas_2_v2_status_update_details as sd2v2;