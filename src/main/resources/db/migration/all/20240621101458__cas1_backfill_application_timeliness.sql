-- This logic mimicks the logic used in the UI code (noticeTypeFromApplication.ts)
with apps_to_update as (
    SELECT
        a.id,
        a.submitted_at,
        -- we use < because the logic in the ui that normally determines this only considers whole days, where
        -- as postgres date comparison will consider 7 days and 1 hour as > 7 days
        -- we truncate the arrival date to days because the API code sets arrival_date to 00:00 UTC,
        -- where as the UI (that determined timeinless) sets it to 00:00 (Europe/London) for comparison
        CASE
            WHEN apa.arrival_date IS NOT NULL AND (date_trunc('day', apa.arrival_date)) - a.submitted_at < '8 days' THEN 'emergency'
            WHEN apa.arrival_date IS NOT NULL AND (date_trunc('day', apa.arrival_date)) - a.submitted_at < '29 days' THEN 'shortNotice'
            ELSE 'standard'
            END as updated_notice_type
    FROM approved_premises_applications apa
             INNER JOIN applications a ON a.id = apa.id
    WHERE a.submitted_at IS NOT NULL and apa.notice_type is null
)

update approved_premises_applications apa
set notice_type = apps_to_update.updated_notice_type
from apps_to_update
where apa.id = apps_to_update.id and apa.notice_type is null;