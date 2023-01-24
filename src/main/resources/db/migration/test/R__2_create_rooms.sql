
-- ${flyway:timestamp}
TRUNCATE TABLE rooms CASCADE;
TRUNCATE TABLE beds CASCADE;

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '14c0911e-6296-4b3f-ad00-5a2cf6f23a08',
      'ROOM1',
      NULL,
      'd33006b7-55d9-4a8e-b722-5e18093dbcdf'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        'fe86a602-6873-49d3-ac3a-3dfef743ae03',
        'default-bed',
        '14c0911e-6296-4b3f-ad00-5a2cf6f23a08'
      );
    

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      'c8848a9c-8340-4e72-a15f-ec327dd23b77',
      'ROOM2',
      NULL,
      'd33006b7-55d9-4a8e-b722-5e18093dbcdf'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        'e8887df9-b31b-4e9c-931a-e063d778ab0d',
        'default-bed',
        'c8848a9c-8340-4e72-a15f-ec327dd23b77'
      );
    

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '2d87a9a2-1f94-45ec-9790-eb8732a4ba6f',
      'ROOM3',
      NULL,
      'd33006b7-55d9-4a8e-b722-5e18093dbcdf'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
        'default-bed',
        '2d87a9a2-1f94-45ec-9790-eb8732a4ba6f'
      );
    

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '088d7993-2cf2-42cd-9993-697b5838e9fd',
      'ROOM4',
      NULL,
      'd33006b7-55d9-4a8e-b722-5e18093dbcdf'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        'd97bdcb9-f7b3-477b-a073-71939fac297a',
        'default-bed',
        '088d7993-2cf2-42cd-9993-697b5838e9fd'
      );
    

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '31b88798-71bb-4698-8479-07c76cf58b8e',
      'ROOM5',
      NULL,
      'd33006b7-55d9-4a8e-b722-5e18093dbcdf'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        '8be1ed0e-dae7-42d2-97e0-95c95fdb4c50',
        'default-bed',
        '31b88798-71bb-4698-8479-07c76cf58b8e'
      );
    

