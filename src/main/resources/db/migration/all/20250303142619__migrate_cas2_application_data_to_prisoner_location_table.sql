insert into cas_2_prisoner_locations (id, application_id, prison_code, staff_id, occurred_at, end_date)
    (select gen_random_uuid(),
            app.id,
            app.referring_prison_code,
            app.created_by_user_id,
            app.submitted_at,
            null
     from cas_2_applications app
     where submitted_at is not null
       and referring_prison_code is not null
       and abandoned_at is not null)