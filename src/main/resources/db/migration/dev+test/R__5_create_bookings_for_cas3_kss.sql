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
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL
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
    'd6447105-4bfe-4f1e-add7-4668e1ca28b0',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
    'temporary-accommodation',
    CURRENT_DATE,
    NULL
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

UPDATE arrivals SET arrival_date_time = cast(arrival_date as timestamp) at time zone 'utc';
