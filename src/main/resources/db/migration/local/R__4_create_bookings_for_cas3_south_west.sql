-- ${flyway:timestamp}

-- CAS3 bookings for first property in the South West

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
    "noms_number"
  )
VALUES
  (
    '1d0f6bcb-b742-4a53-8988-aeb192350824',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    'X371199',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    'b9ae343d-38b4-46db-9ca6-46572a347791',
    'f45a897a-5831-49d8-81bd-050368fd3bf2',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL
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
    "noms_number"
  )
VALUES
  (
    '1de846dd-9617-4488-9b05-d54c9f955e2b',
    CURRENT_DATE + 84 ,
    CURRENT_DATE + 168,
    'X698320',
    CURRENT_DATE + 84,
    CURRENT_DATE + 168,
    'b9ae343d-38b4-46db-9ca6-46572a347791',
    '47c8aa9c-b024-43db-b3d2-4bc867fd4f73',
    'temporary-accommodation',
    CURRENT_DATE + 80,
    NULL
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

--- Add a Booking departing today ---

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
    "noms_number"
  )
VALUES
  (
    '713943e2-1ae4-4101-aa6c-9a40e7930168',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    'X698319', -- Missing NOMIS and OASys data
    CURRENT_DATE - 84,
    CURRENT_DATE,
    'b9ae343d-38b4-46db-9ca6-46572a347791',
    'a1ae650f-862e-4347-9440-8b84086f7ca3',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL
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
  lost_beds (
    "id",
    "premises_id",
    "start_date",
    "end_date",
    "reference_number",
    "notes",
    "lost_bed_reason_id",
    "bed_id"
  )
VALUES
  (
    'c0417398-b3a2-4144-ad36-01e206b8caa9',
    'b9ae343d-38b4-46db-9ca6-46572a347791',
    CURRENT_DATE + 200,
    CURRENT_DATE + 205,
    '132',
    'Some notes for a void',
    (SELECT id FROM lost_bed_reasons WHERE name='Deep clean'),
    'f45a897a-5831-49d8-81bd-050368fd3bf2'
  )
ON CONFLICT(id) DO NOTHING;
--- Add a Booking departing soon ---

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
    "noms_number"
  )
VALUES
  (
    '13145d61-6ea7-4e77-9c89-cbbd2f7d85c4',
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    'X698334', -- LAO
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    'b9ae343d-38b4-46db-9ca6-46572a347791',
    '30f4156d-0ec6-422f-a896-de3c3b256376',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL
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
    "noms_number"
  )
VALUES
  (
    'a6fc549a-915d-4884-97f0-bc1cee84208c',
    CURRENT_DATE - 7,
    CURRENT_DATE + 51,
    'X698320', -- Multiple offences
    CURRENT_DATE - 7,
    CURRENT_DATE + 51,
    'b9ae343d-38b4-46db-9ca6-46572a347791',
    'f45a897a-5831-49d8-81bd-050368fd3bf2',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL
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
    "noms_number"
  )
VALUES
  (
    'e3a08fac-88b3-4691-ad6f-b26b9180b1c6',
    CURRENT_DATE - 168,
    CURRENT_DATE - 1,
    'X320741',
    CURRENT_DATE - 168,
    CURRENT_DATE - 1,
    'b9ae343d-38b4-46db-9ca6-46572a347791',
    'f45a897a-5831-49d8-81bd-050368fd3bf2',
    'temporary-accommodation',
    CURRENT_DATE - 200,
    NULL
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
