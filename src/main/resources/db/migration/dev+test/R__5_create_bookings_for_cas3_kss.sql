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
    "noms_number"
  )
VALUES
  (
    '80d5337f-4af4-4df9-a1ae-bb909444715f',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    'X371199',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    'd6447105-4bfe-4f1e-add7-4668e1ca28b0',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
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
    'cc3b1ef5-b492-4771-9cb9-975cf36c7f19',
    '80d5337f-4af4-4df9-a1ae-bb909444715f',
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
    'd6447105-4bfe-4f1e-add7-4668e1ca28b0',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
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
    'ec0580b0-9c93-4114-b5c5-5e9da19af5d3',
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
    'd6447105-4bfe-4f1e-add7-4668e1ca28b0',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
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
    'f1f6801f-a900-4acf-9908-7b4568e23ec1',
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
    'b9c72631-6a35-489d-aee2-ea3d4498940e',
    'd6447105-4bfe-4f1e-add7-4668e1ca28b0',
    CURRENT_DATE + 200,
    CURRENT_DATE + 205,
    '132',
    'Some notes for a void',
    (SELECT id FROM lost_bed_reasons WHERE name='Deep clean'),
    'e8887df9-b31b-4e9c-931a-e063d778ab0d'
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
    'X698234', -- Multiple Offences
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    'd6447105-4bfe-4f1e-add7-4668e1ca28b0',
    '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
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
    'c9c40961-5fe8-46e7-bf9c-38d2daf4074a',
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
    'X698334', -- Multiple offences
    CURRENT_DATE - 7,
    CURRENT_DATE + 51,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
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
    '9c209338-9a57-43da-a35e-0dd22db4240f',
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
    'd6447105-4bfe-4f1e-add7-4668e1ca28b0',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
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
    '49d5cdc3-bd54-4954-8dcb-ee4fd45ba573',
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
