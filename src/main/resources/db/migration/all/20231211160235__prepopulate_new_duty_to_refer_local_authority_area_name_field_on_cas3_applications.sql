UPDATE temporary_accommodation_applications AS ta
SET duty_to_refer_local_authority_area_name = (
    SELECT app.data->'accommodation-referral-details'->'dtr-details'->>'localAuthorityAreaName'
    FROM applications AS app
    WHERE app.id = ta.id AND service ='temporary-accommodation'
)
WHERE duty_to_refer_local_authority_area_name IS NULL;