-- ${flyway:timestamp}

-- CAS3 bookings for first property in the East of England

--- Add a Booking arriving soon ---

INSERT INTO
  bookings (
    "id",
    "arrival_date",
    "departure_date",
    "crn",
    "original_arrival_date",
    "original_departure_date",
    "premises_id",
    "bed_id",
    "service",
    "created_at",
    "noms_number",
    "status"
  )
VALUES
  (
    '1d0f6bcb-b742-4a53-8988-aeb192350824',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    'X371199',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '70a6046c-23fc-4a30-b151-582ffd509e6a',
    '38e6b775-88c5-4571-8b6e-da3711aeaca6',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL,
    'provisional'
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  turnarounds (
    "id",
    "booking_id",
    "working_day_count",
    "created_at"
  )
VALUES
  (
    'c30a8ddb-fd44-40ac-b2ba-5ce5c15e110a',
    '1d0f6bcb-b742-4a53-8988-aeb192350824',
    2,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add a confirmed booking ---

INSERT INTO
  bookings (
    "id",
    "arrival_date",
    "departure_date",
    "crn",
    "original_arrival_date",
    "original_departure_date",
    "premises_id",
    "bed_id",
    "service",
    "created_at",
    "noms_number",
    "status"
  )
VALUES
  (
    '1de846dd-9617-4488-9b05-d54c9f955e2b',
    CURRENT_DATE + 84 ,
    CURRENT_DATE + 168,
    'X698320',
    CURRENT_DATE + 84,
    CURRENT_DATE + 168,
    '70a6046c-23fc-4a30-b151-582ffd509e6a',
    '38e6b775-88c5-4571-8b6e-da3711aeaca6',
    'temporary-accommodation',
    CURRENT_DATE + 80,
    NULL,
    'confirmed'
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  turnarounds (
    "id",
    "booking_id",
    "working_day_count",
    "created_at"
  )
VALUES
  (
    'ec8d595a-25db-48cd-b9a0-2576998825a5',
    '1de846dd-9617-4488-9b05-d54c9f955e2b',
    2,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  confirmations (
    "id",
    "booking_id",
    "date_time",
    "notes",
    "created_at"
  )
VALUES
  (
    '67f339b7-dca4-476b-ad9c-7db1f857465f',
    '1de846dd-9617-4488-9b05-d54c9f955e2b',
    CURRENT_DATE,
    NULL,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add a Booking arrivals today ---

INSERT INTO
  bookings (
    "id",
    "arrival_date",
    "departure_date",
    "crn",
    "original_arrival_date",
    "original_departure_date",
    "premises_id",
    "bed_id",
    "service",
    "created_at",
    "noms_number",
    "status"
  )
VALUES
  (
    '713943e2-1ae4-4101-aa6c-9a40e7930168',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    'X698319', -- Missing NOMIS and OASys data
    CURRENT_DATE - 84,
    CURRENT_DATE,
    '70a6046c-23fc-4a30-b151-582ffd509e6a',
    '38e6b775-88c5-4571-8b6e-da3711aeaca6',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL,
    'arrived'
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  turnarounds (
    "id",
    "booking_id",
    "working_day_count",
    "created_at"
  )
VALUES
  (
    '96ef4637-a768-457f-b803-aa22343d4934',
    '713943e2-1ae4-4101-aa6c-9a40e7930168',
    2,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  arrivals (
    "arrival_date",
    "booking_id",
    "created_at",
    "expected_departure_date",
    "id",
    "notes"
  )
VALUES
  (
    CURRENT_DATE - 84,
    '713943e2-1ae4-4101-aa6c-9a40e7930168',
    CURRENT_DATE,
    CURRENT_DATE,
    'c029c7fb-698b-43d9-b389-16130013d95d',
    'Missing NOMIS and OASys data'
  )
ON CONFLICT(id) DO NOTHING;

--- Add a void ---

INSERT INTO
    cas3_void_bedspaces (
    "id",
    "premises_id",
    "start_date",
    "end_date",
    "reference_number",
    "notes",
    "cas3_void_bedspace_reason_id",
    "bed_id"
  )
VALUES
  (
    'c0417398-b3a2-4144-ad36-01e206b8caa9',
    '70a6046c-23fc-4a30-b151-582ffd509e6a',
    CURRENT_DATE + 200,
    CURRENT_DATE + 205,
    '132',
    'Some notes for a void',
    (SELECT id FROM cas3_void_bedspace_reasons WHERE name='Deep clean'),
    '38e6b775-88c5-4571-8b6e-da3711aeaca6'
  )
ON CONFLICT(id) DO NOTHING;
--- Add a Booking arrived soon ---

INSERT INTO
  bookings (
    "id",
    "arrival_date",
    "departure_date",
    "crn",
    "original_arrival_date",
    "original_departure_date",
    "premises_id",
    "bed_id",
    "service",
    "created_at",
    "noms_number",
    "status"
  )
VALUES
  (
    '13145d61-6ea7-4e77-9c89-cbbd2f7d85c4',
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    'X698334', -- LAO
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    '70a6046c-23fc-4a30-b151-582ffd509e6a',
    'fd1c7078-43c8-41f5-8e57-a4d59f3c831a',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL,
    'arrived'
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  turnarounds (
    "id",
    "booking_id",
    "working_day_count",
    "created_at"
  )
VALUES
  (
    '312f3b79-c3b3-4792-8e63-c86f6f9c3f78',
    '13145d61-6ea7-4e77-9c89-cbbd2f7d85c4',
    2,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  arrivals (
    "arrival_date",
    "booking_id",
    "created_at",
    "expected_departure_date",
    "id",
    "notes"
  )
VALUES
  (
    CURRENT_DATE - 84,
    '13145d61-6ea7-4e77-9c89-cbbd2f7d85c4',
    CURRENT_DATE,
    CURRENT_DATE + 3,
    '35cafa4e-c104-4848-a1c9-9163aea6cae0',
    'Multiple Offences'
  )
ON CONFLICT(id) DO NOTHING;

--- Add some arrived Bookings ---

INSERT INTO
  bookings (
    "id",
    "arrival_date",
    "departure_date",
    "crn",
    "original_arrival_date",
    "original_departure_date",
    "premises_id",
    "bed_id",
    "service",
    "created_at",
    "noms_number",
    "status"
  )
VALUES
  (
    'a6fc549a-915d-4884-97f0-bc1cee84208c',
    CURRENT_DATE - 7,
    CURRENT_DATE + 51,
    'X698320', -- Multiple offences
    CURRENT_DATE - 7,
    CURRENT_DATE + 51,
    '6aa177cb-617f-4abb-be46-056ea7e4a59d',
    '64fd8f3d-1fb6-4346-a190-65588b998301',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL,
    'arrived'
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  turnarounds (
    "id",
    "booking_id",
    "working_day_count",
    "created_at"
  )
VALUES
  (
    '60f56560-7499-4c60-bae8-63ea7cbe685e',
    'a6fc549a-915d-4884-97f0-bc1cee84208c',
    2,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  arrivals (
    "arrival_date",
    "booking_id",
    "created_at",
    "expected_departure_date",
    "id",
    "notes"
  )
VALUES
  (
    CURRENT_DATE - 7,
    'a6fc549a-915d-4884-97f0-bc1cee84208c',
    CURRENT_DATE,
    CURRENT_DATE + 51,
    'aa67b195-ee25-4944-8e97-56af5a3e1c2f',
    'Multiple offences'
  )
ON CONFLICT(id) DO NOTHING;

--- Add a departure ---

INSERT INTO
  bookings (
    "id",
    "arrival_date",
    "departure_date",
    "crn",
    "original_arrival_date",
    "original_departure_date",
    "premises_id",
    "bed_id",
    "service",
    "created_at",
    "noms_number",
    "status"
  )
VALUES
  (
    'e3a08fac-88b3-4691-ad6f-b26b9180b1c6',
    CURRENT_DATE - 168,
    CURRENT_DATE - 1,
    'X320741',
    CURRENT_DATE - 168,
    CURRENT_DATE - 1,
    '70a6046c-23fc-4a30-b151-582ffd509e6a',
    '38e6b775-88c5-4571-8b6e-da3711aeaca6',
    'temporary-accommodation',
    CURRENT_DATE - 200,
    NULL,
    'departed'
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  turnarounds (
    "id",
    "booking_id",
    "working_day_count",
    "created_at"
  )
VALUES
  (
    '84807d77-1ccb-408a-b307-df79980791cd',
    'e3a08fac-88b3-4691-ad6f-b26b9180b1c6',
    2,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

INSERT INTO
  departures (
    "id",
    "date_time",
    "departure_reason_id",
    "move_on_category_id",
    "destination_provider_id",
    "notes",
    "booking_id",
    "created_at"
  )
VALUES
  (
    '5a1380a3-ab28-47a4-aab9-60414701f804',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    'e3a08fac-88b3-4691-ad6f-b26b9180b1c6',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

UPDATE arrivals SET arrival_date_time = cast(arrival_date as timestamp) at time zone 'utc';
