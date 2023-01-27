
-- ${flyway:timestamp}


  insert into
    users (
      "delius_staff_identifier",
      "delius_username",
      "id",
      "name",
      "probation_region_id"
    )
  values
    (
      '54281',
      'AP_USER_TEST_1',
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      'AP Test User 1',
      '43606be0-9836-441d-9bc1-5586de9ac931'
    )
  ON CONFLICT (id) DO NOTHING;


  insert into
    users (
      "delius_staff_identifier",
      "delius_username",
      "id",
      "name",
      "probation_region_id"
    )
  values
    (
      '34260',
      'AP_USER_TEST_2',
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      'AP Test User 2',
      '43606be0-9836-441d-9bc1-5586de9ac931'
    )
  ON CONFLICT (id) DO NOTHING;


  insert into
    users (
      "delius_staff_identifier",
      "delius_username",
      "id",
      "name",
      "probation_region_id"
    )
  values
    (
      '8522',
      'AP_USER_TEST_3',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      'AP Test User 3',
      '43606be0-9836-441d-9bc1-5586de9ac931'
    )
  ON CONFLICT (id) DO NOTHING;


  insert into
    users (
      "delius_staff_identifier",
      "delius_username",
      "id",
      "name",
      "probation_region_id"
    )
  values
    (
      '3210',
      'AP_USER_TEST_4',
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      'AP Test User 4',
      '43606be0-9836-441d-9bc1-5586de9ac931'
    )
  ON CONFLICT (id) DO NOTHING;


  insert into
    users (
      "delius_staff_identifier",
      "delius_username",
      "id",
      "name",
      "probation_region_id"
    )
  values
    (
      '57009',
      'AP_USER_TEST_5',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      'AP Test User 5',
      '43606be0-9836-441d-9bc1-5586de9ac931'
    )
  ON CONFLICT (id) DO NOTHING;


  insert into
    users (
      "delius_staff_identifier",
      "delius_username",
      "id",
      "name",
      "probation_region_id"
    )
  values
    (
      '10088',
      'CAS_NCC_TEST1',
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
      'NCC User 1',
      '43606be0-9836-441d-9bc1-5586de9ac931'
    )
  ON CONFLICT (id) DO NOTHING;


  insert into
    users (
      "delius_staff_identifier",
      "delius_username",
      "id",
      "name",
      "probation_region_id"
    )
  values
    (
      '27207',
      'CAS_NCC_TEST2',
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
      'NCC User 2',
      '43606be0-9836-441d-9bc1-5586de9ac931'
    )
  ON CONFLICT (id) DO NOTHING;


