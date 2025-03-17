DELETE FROM cas_2_application_assignments;

INSERT INTO cas_2_application_assignments (id, application_id, prison_code, allocated_pom_user_id, created_at)
    (SELECT gen_random_uuid(),
                                  app.id,
                                  app.referring_prison_code,
                                  app.created_by_user_id,
                                  app.submitted_at
     FROM cas_2_applications app
     WHERE submitted_at IS NOT NULL
       AND abandoned_at IS NULL);
