-- ${flyway:timestamp}
-- ideally this would only be managed via the seed CSV files but removing the contents of this file breaks R__5_create_bookings_for_cas3 as they require the beds
-- to exist at that point in time. The correct solution is to move this logic into the SeedOnStartupService

TRUNCATE TABLE rooms CASCADE;
TRUNCATE TABLE beds CASCADE;

-- CAS3 beds for Kent, Surrey & Sussex

-- Premises 1 (2x rooms, 1x bed each)
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

-- Premises 2 (1x room, 1x bed)
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
        '6d6d4c56-9989-4fb5-a486-d32f525748e6',
        'BED1',
        '2d87a9a2-1f94-45ec-9790-eb8732a4ba6f'
    );

-- Premises 3 (1x room, 1x bed)
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

-- CAS3 beds for East of England

-- Premises 1 (2x rooms, 1x bed each)
insert into
    rooms ("id", "name", "notes", "premises_id")
values
    (
        '4d27144d-1c3d-4785-9fbd-879d8b0b5b41',
        'EOFE-1',
        NULL,
        '70a6046c-23fc-4a30-b151-582ffd509e6a'
    );

insert into
    beds ("id", "name", "room_id")
values
    (
        '38e6b775-88c5-4571-8b6e-da3711aeaca6',
        'BED1',
        '4d27144d-1c3d-4785-9fbd-879d8b0b5b41'
    );

insert into
    rooms ("id", "name", "notes", "premises_id")
values
    (
        '94c58e72-d31a-49c3-8569-fc341e46ba6a',
        'EOFE-2',
        NULL,
        '70a6046c-23fc-4a30-b151-582ffd509e6a'
    );

insert into
    beds ("id", "name", "room_id")
values
    (
        'fd1c7078-43c8-41f5-8e57-a4d59f3c831a',
        'BED1',
        '94c58e72-d31a-49c3-8569-fc341e46ba6a'
    );

-- Premises 2 (1x room, 1x bed)
insert into
    rooms ("id", "name", "notes", "premises_id")
values
    (
        '82ff4d7f-13c2-4827-8957-c38ad4750c53',
        'A1',
        NULL,
        '6aa177cb-617f-4abb-be46-056ea7e4a59d'
    );
insert into
    beds ("id", "name", "room_id")
values
    (
        '64fd8f3d-1fb6-4346-a190-65588b998301',
        'BED1',
        '82ff4d7f-13c2-4827-8957-c38ad4750c53'
    );

-- Premises 2 (1x room, 1x bed)
insert into
    rooms ("id", "name", "notes", "premises_id")
values
    (
        '02e1c7a3-47e7-4845-95f5-98aeed0ef81b',
        'A1',
        NULL,
        '773431cd-f560-4be8-9e6f-b582a4ebf204'
    );
insert into
    beds ("id", "name", "room_id")
values
    (
        '8ecef9a5-268c-4595-9fd0-042fed3d4882',
        'BED1',
        '02e1c7a3-47e7-4845-95f5-98aeed0ef81b'
    );
