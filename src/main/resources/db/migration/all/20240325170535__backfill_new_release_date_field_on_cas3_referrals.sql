UPDATE temporary_accommodation_applications AS ta
SET person_release_date = (
    SELECT TO_DATE(app.data->'eligibility'->'release-date'->>'releaseDate','YYYY-MM-DD')
    FROM applications AS app
    WHERE app.id = ta.id AND service ='temporary-accommodation' And app.data is not null
)
WHERE person_release_date IS NULL;