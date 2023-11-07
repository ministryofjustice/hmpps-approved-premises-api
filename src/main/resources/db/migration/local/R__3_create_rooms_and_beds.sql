-- ${flyway:timestamp}

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
    )
  on conflict(id) do nothing;

  insert into
    beds ("id", "name", "room_id")
  values
    (
      'e8887df9-b31b-4e9c-931a-e063d778ab0d',
      'BED1',
      '14c0911e-6296-4b3f-ad00-5a2cf6f23a08'
    )
  on conflict(id) do nothing;

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      'fe86a602-6873-49d3-ac3a-3dfef743ae03',
      'ROOM2',
      NULL,
      'd6447105-4bfe-4f1e-add7-4668e1ca28b0'
    )
  on conflict(id) do nothing;
  insert into
    beds ("id", "name", "room_id")
  values
    (
      '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
      'BED1',
      'fe86a602-6873-49d3-ac3a-3dfef743ae03'
    )
  on conflict(id) do nothing;

--      Premises 2 (1x room, 1x bed)
  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '2d87a9a2-1f94-45ec-9790-eb8732a4ba6f',
      'ROOM1',
      NULL,
      'e2543d2f-33a9-454b-ae15-03ca0475faa3'
    )
  on conflict(id) do nothing;
  insert into
    beds ("id", "name", "room_id")
  values
    (
      'd97bdcb9-f7b3-477b-a073-71939fac297a',
      'BED1',
      '2d87a9a2-1f94-45ec-9790-eb8732a4ba6f'
    )
  on conflict(id) do nothing;

--      Premises 3 (1x room, 1x bed)
  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
      'ROOM1',
      NULL,
      '0ad5999f-a07c-4605-b875-81d7a17e9f70'
    )
  on conflict(id) do nothing;
  insert into
    beds ("id", "name", "room_id")
  values
    (
      '8be1ed0e-dae7-42d2-97e0-95c95fdb4c50',
      'BED1',
      '135812b4-e6c0-4ccf-9502-4bfea66f3bd3'
    )
  on conflict(id) do nothing;

--      Premises 4 South West
  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '7a2efcd0-77c4-4fd0-aa23-5f59ff212dea',
      'ROOM 1',
      NULL,
      'b9ae343d-38b4-46db-9ca6-46572a347791'
    )
  on conflict(id) do nothing;

  insert into
    beds ("id", "name", "room_id")
  values
    (
      'f45a897a-5831-49d8-81bd-050368fd3bf2',
      'BED 1',
      '7a2efcd0-77c4-4fd0-aa23-5f59ff212dea'
    )
  on conflict(id) do nothing;

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      '97dafa5b-d52c-425a-a3a6-db50ba760eca',
      'ROOM 2',
      NULL,
      'b9ae343d-38b4-46db-9ca6-46572a347791'
    )
  on conflict(id) do nothing;

  insert into
    beds ("id", "name", "room_id")
  values
    (
      '47c8aa9c-b024-43db-b3d2-4bc867fd4f73',
      'BED 1',
      '97dafa5b-d52c-425a-a3a6-db50ba760eca'
    )
  on conflict(id) do nothing;

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      'bceb1b63-18f7-4ec7-9010-60d820393915',
      'ROOM 3',
      NULL,
      'b9ae343d-38b4-46db-9ca6-46572a347791'
    )
  on conflict(id) do nothing;

  insert into
    beds ("id", "name", "room_id")
  values
    (
      'a1ae650f-862e-4347-9440-8b84086f7ca3',
      'BED 1',
      'bceb1b63-18f7-4ec7-9010-60d820393915'
    )
  on conflict(id) do nothing;

  insert into
    rooms ("id", "name", "notes", "premises_id")
  values
    (
      'ecaef2ee-aac2-4df5-9e5b-666bf90b3ca1',
      'ROOM 4',
      NULL,
      'b9ae343d-38b4-46db-9ca6-46572a347791'
    )
  on conflict(id) do nothing;

  insert into
    beds ("id", "name", "room_id")
  values
    (
      '30f4156d-0ec6-422f-a896-de3c3b256376',
      'BED 1',
      'ecaef2ee-aac2-4df5-9e5b-666bf90b3ca1'
    )
  on conflict(id) do nothing;
