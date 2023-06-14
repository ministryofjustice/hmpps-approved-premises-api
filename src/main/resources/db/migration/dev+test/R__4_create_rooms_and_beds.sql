-- ${flyway:timestamp}

TRUNCATE TABLE rooms CASCADE;
TRUNCATE TABLE beds CASCADE;

-- temporary_accommodation_premises
--      Premises 1 (2x rooms, 1x bed each)
  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '14c0911e-6296-4b3f-ad00-5a2cf6f23a08',
      'ROOM1',
      NULL,
      'd6447105-4bfe-4f1e-add7-4668e1ca28b0'
    );

  insert into
    beds ("id", "name", "room_id")
  values
    (
      'e8887df9-b31b-4e9c-931a-e063d778ab0d',
      'BED1',
      '14c0911e-6296-4b3f-ad00-5a2cf6f23a08'
    );

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      'fe86a602-6873-49d3-ac3a-3dfef743ae03',
      'ROOM2',
      NULL,
      'd6447105-4bfe-4f1e-add7-4668e1ca28b0'
    );
  insert into
    beds ("id", "name", "room_id")
  values
    (
      '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
      'BED1',
      'fe86a602-6873-49d3-ac3a-3dfef743ae03'
    );

--      Premises 2 (1x room, 1x bed)
  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '2d87a9a2-1f94-45ec-9790-eb8732a4ba6f',
      'ROOM1',
      NULL,
      'e2543d2f-33a9-454b-ae15-03ca0475faa3'
    );
  insert into
    beds ("id", "name", "room_id")
  values
    (
      'd97bdcb9-f7b3-477b-a073-71939fac297a',
      'BED1',
      '2d87a9a2-1f94-45ec-9790-eb8732a4ba6f'
    );

--      Premises 3 (1x room, 1x bed)
  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
      'ROOM1',
      NULL,
      '0ad5999f-a07c-4605-b875-81d7a17e9f70'
    );
  insert into
    beds ("id", "name", "room_id")
  values
    (
      '8be1ed0e-dae7-42d2-97e0-95c95fdb4c50',
      'BED1',
      '135812b4-e6c0-4ccf-9502-4bfea66f3bd3'
    );
