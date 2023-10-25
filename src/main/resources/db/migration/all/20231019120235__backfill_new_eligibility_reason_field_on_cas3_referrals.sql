UPDATE temporary_accommodation_applications AS ta
SET eligibility_reason = (
    SELECT app.data->'eligibility'->'eligibility-reason'->>'reason'
    FROM applications AS app
    WHERE app.id = ta.id AND service ='temporary-accommodation'
)
WHERE eligibility_reason IS NULL;
