-- ${flyway:timestamp}
-- ideally this would only be managed via the seed CSV files but removing the contents of this file breaks R__7_create_bookings_for_cas3 as they require the beds
-- to exist at that point in time. The correct solution is to move this logic into the SeedOnStartupService

-- CAS3 bedspaces for Kent, Surrey & Sussex

-- Premises 1 (2x bedspaces)
insert into cas3_bedspaces (id, premises_id, reference, start_date, end_date, notes, created_date)
values ('e8887df9-b31b-4e9c-931a-e063d778ab0d','d6447105-4bfe-4f1e-add7-4668e1ca28b0','ROOM1','01/06/2024',null,null, now())
ON CONFLICT (id) DO NOTHING;

insert into cas3_bedspaces (id, premises_id, reference, start_date, end_date, notes, created_date)
values( '135812b4-e6c0-4ccf-9502-4bfea66f3bd3', 'd6447105-4bfe-4f1e-add7-4668e1ca28b0','ROOM2','01/07/2024',null,null, now())
ON CONFLICT (id) DO NOTHING;

-- Premises 2 (1x bedspace)
insert into cas3_bedspaces (id, premises_id, reference, start_date, end_date, notes, created_date)
values ( '6d6d4c56-9989-4fb5-a486-d32f525748e6','e2543d2f-33a9-454b-ae15-03ca0475faa3','ROOM1','01/08/2024',null,null, now())
ON CONFLICT (id) DO NOTHING;

-- Premises 3 (1x bedspace)
insert into cas3_bedspaces (id, premises_id, reference, start_date, end_date, notes, created_date)
values ('8be1ed0e-dae7-42d2-97e0-95c95fdb4c50', '0ad5999f-a07c-4605-b875-81d7a17e9f70','ROOM1','01/09/2024',null,null, now())
ON CONFLICT (id) DO NOTHING;

-- CAS3 bedspaces for East of England

-- Premises 4 (2x bedspaces)
insert into cas3_bedspaces (id, premises_id, reference, start_date, end_date, notes, created_date)
values ('38e6b775-88c5-4571-8b6e-da3711aeaca6','70a6046c-23fc-4a30-b151-582ffd509e6a','EOFE-1','01/10/2024',null,null, now())
ON CONFLICT (id) DO NOTHING;

insert into cas3_bedspaces (id, premises_id, reference, start_date, end_date, notes, created_date)
values ('fd1c7078-43c8-41f5-8e57-a4d59f3c831a','70a6046c-23fc-4a30-b151-582ffd509e6a', 'EOFE-2', '01/11/2024',null,null, now())
ON CONFLICT (id) DO NOTHING;

-- Premises 5 (1x bedspace)
insert into cas3_bedspaces (id, premises_id, reference, start_date, end_date, notes, created_date)
values ('64fd8f3d-1fb6-4346-a190-65588b998301','6aa177cb-617f-4abb-be46-056ea7e4a59d','A1', '01/12/2024',null, null, now())
ON CONFLICT (id) DO NOTHING;

-- Premises 6 (1x bedspace)
insert into cas3_bedspaces (id, premises_id, reference, start_date, end_date, notes, created_date)
values ( '8ecef9a5-268c-4595-9fd0-042fed3d4882','773431cd-f560-4be8-9e6f-b582a4ebf204', 'A1','01/01/2025',null,null, now())
ON CONFLICT (id) DO NOTHING;
