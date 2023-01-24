
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
    

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      'a82a64f0-a9b6-4854-a5c6-eecb16d8724a',
      'ROOM6',
      NULL,
      'b6a87a7e-1b6a-4c96-b7ff-8c0dc414796d'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        'bdf9f9f6-6d53-4577-bffe-fc5f0ab3de0f',
        'default-bed',
        'a82a64f0-a9b6-4854-a5c6-eecb16d8724a'
      );
    

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '1363dca9-baac-46d4-b045-1da39b7aa46d',
      'ROOM7',
      NULL,
      'ada106c7-e1fb-409a-a38e-0002ea8e7e45'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        '055f1f4a-4f86-4ed1-80dc-b793f96c3003',
        'default-bed',
        '1363dca9-baac-46d4-b045-1da39b7aa46d'
      );
    

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '53ab0bb3-1553-46e8-877b-6ce6a7ee5d3c',
      'ROOM8',
      NULL,
      'ada106c7-e1fb-409a-a38e-0002ea8e7e45'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        '5f307ee5-e53a-47b3-85b1-a85f0436d9bb',
        'default-bed',
        '53ab0bb3-1553-46e8-877b-6ce6a7ee5d3c'
      );
    

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '6f8f50e9-0467-46fa-964b-20d901078631',
      'ROOM9',
      NULL,
      '36c7b1f2-5a4b-467b-838c-2970c9c253cf'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        'a9c6a648-bb73-4d03-b670-a01e3d67dc7a',
        'default-bed',
        '6f8f50e9-0467-46fa-964b-20d901078631'
      );
    

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '390883bb-9689-4e17-bfd6-2a762ae182a2',
      'ROOM10',
      NULL,
      '36c7b1f2-5a4b-467b-838c-2970c9c253cf'
    );
  
    insert into
        beds ("id", "name", "room_id")
    values
      (
        '8eb3fa96-48fe-4e24-8015-16bf009ad5ca',
        'default-bed',
        '390883bb-9689-4e17-bfd6-2a762ae182a2'
      );
    

