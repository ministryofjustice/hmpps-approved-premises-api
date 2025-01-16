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
    'b9c72631-6a35-489d-aee2-ea3d4498940e',
    'd6447105-4bfe-4f1e-add7-4668e1ca28b0',
    CURRENT_DATE + 200,
    CURRENT_DATE + 205,
    '132',
    'Some notes for a void',
    (SELECT id FROM cas3_void_bedspace_reasons WHERE name='Deep clean'),
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

--- Add some departed Bookings for E2E---

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
    'b8d2b571-b5ce-4df4-8e99-f65a86470ef0',
    CURRENT_DATE - (5 * 2),
    CURRENT_DATE - (5 * 2) - 2,
    'X371199',
    CURRENT_DATE - (5 * 2),
    CURRENT_DATE - (5 * 2) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    'c78bb479-d2a8-4e2d-b800-58d9602bf6e5',
    'b8d2b571-b5ce-4df4-8e99-f65a86470ef0',
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
    '12f1e431-649d-4789-a6ca-1b4e12bf6313',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    'b8d2b571-b5ce-4df4-8e99-f65a86470ef0',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    'dd833ba5-2924-4bda-8e5f-6036c2eef8c8',
    CURRENT_DATE - (5 * 3),
    CURRENT_DATE - (5 * 3) - 2,
    'X371199',
    CURRENT_DATE - (5 * 3),
    CURRENT_DATE - (5 * 3) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    '91b1cc3b-0849-4c75-a7c4-03a344c4ec4e',
    'dd833ba5-2924-4bda-8e5f-6036c2eef8c8',
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
    '0bdce140-af2d-4983-867f-658d0b3c3578',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    'dd833ba5-2924-4bda-8e5f-6036c2eef8c8',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    '10df1538-39d3-4f69-af32-53907f020f61',
    CURRENT_DATE - (5 * 4),
    CURRENT_DATE - (5 * 4) - 2,
    'X371199',
    CURRENT_DATE - (5 * 4),
    CURRENT_DATE - (5 * 4) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    'ce634a9d-878d-4e2c-a57b-780361138550',
    '10df1538-39d3-4f69-af32-53907f020f61',
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
    '9e4bf10c-7c92-4691-890c-31e50a26671c',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '10df1538-39d3-4f69-af32-53907f020f61',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    '013d0da0-9dc9-4aa5-a95a-be23f67cb20c',
    CURRENT_DATE - (5 * 5),
    CURRENT_DATE - (5 * 5) - 2,
    'X371199',
    CURRENT_DATE - (5 * 5),
    CURRENT_DATE - (5 * 5) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    '99f8f729-e358-4928-a7bf-a6129f63be99',
    '013d0da0-9dc9-4aa5-a95a-be23f67cb20c',
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
    'e3dbb6b6-7b2e-4c97-a153-5d0357f48ae2',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '013d0da0-9dc9-4aa5-a95a-be23f67cb20c',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    'f65006c9-2d6f-49f9-a800-3e3b71b9db72',
    CURRENT_DATE - (5 * 6),
    CURRENT_DATE - (5 * 6) - 2,
    'X371199',
    CURRENT_DATE - (5 * 6),
    CURRENT_DATE - (5 * 6) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    '81c80f82-e3c7-469a-b3f0-8f03e7f8cb37',
    'f65006c9-2d6f-49f9-a800-3e3b71b9db72',
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
    '6c7b29af-59e6-48d4-877d-e98a06345233',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    'f65006c9-2d6f-49f9-a800-3e3b71b9db72',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    '4096917e-42ff-4adb-b141-bbd39ac19ce7',
    CURRENT_DATE - (5 * 7),
    CURRENT_DATE - (5 * 7) - 2,
    'X371199',
    CURRENT_DATE - (5 * 7),
    CURRENT_DATE - (5 * 7) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    '2fb9ceff-7135-47fe-ad58-14781b4ed0f4',
    '4096917e-42ff-4adb-b141-bbd39ac19ce7',
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
    '415011bd-ae3b-4cda-bc43-bf74dbc880f4',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '4096917e-42ff-4adb-b141-bbd39ac19ce7',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    'c97bb88b-da6a-4a96-8c98-664b0bebd9e1',
    CURRENT_DATE - (5 * 8),
    CURRENT_DATE - (5 * 8) - 2,
    'X371199',
    CURRENT_DATE - (5 * 8),
    CURRENT_DATE - (5 * 8) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    '6c40948a-12b4-4353-a093-48df24d624df',
    'c97bb88b-da6a-4a96-8c98-664b0bebd9e1',
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
    '80fc303c-23a9-4aa1-a04d-53463cbf0b2e',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    'c97bb88b-da6a-4a96-8c98-664b0bebd9e1',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    '70ba95d6-d3e1-4a1b-85bc-8f37fbf8618f',
    CURRENT_DATE - (5 * 8),
    CURRENT_DATE - (5 * 8) - 2,
    'X371199',
    CURRENT_DATE - (5 * 8),
    CURRENT_DATE - (5 * 8) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    '5ce549ae-e079-4d0e-8476-521252a17361',
    '70ba95d6-d3e1-4a1b-85bc-8f37fbf8618f',
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
    'cd51ea31-774b-478b-9b18-5addcdf2020e',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '70ba95d6-d3e1-4a1b-85bc-8f37fbf8618f',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    '9eec3324-c5db-4983-9907-cdd12890dd39',
    CURRENT_DATE - (5 * 9),
    CURRENT_DATE - (5 * 9) - 2,
    'X371199',
    CURRENT_DATE - (5 * 9),
    CURRENT_DATE - (5 * 9) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    'bb493404-4aa5-4272-86cf-03f385f65c33',
    '9eec3324-c5db-4983-9907-cdd12890dd39',
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
    '90cd2885-a26e-40af-9dd8-d4cb1260b0bb',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '9eec3324-c5db-4983-9907-cdd12890dd39',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    '99d8f177-ab9a-4afc-a5b9-360ccd40c5dc',
    CURRENT_DATE - (5 * 10),
    CURRENT_DATE - (5 * 10) - 2,
    'X371199',
    CURRENT_DATE - (5 * 10),
    CURRENT_DATE - (5 * 10) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    '7e8e9978-47e5-4a78-8819-46f9af3723bb',
    '99d8f177-ab9a-4afc-a5b9-360ccd40c5dc',
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
    '40d1a31d-61a7-46be-bbde-9c02463f4c0d',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '99d8f177-ab9a-4afc-a5b9-360ccd40c5dc',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

--- Add some departed Bookings for E2E---

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
    '541ab042-3b7b-4fb2-b1be-a36b42581ec3',
    CURRENT_DATE - (5 * 11),
    CURRENT_DATE - (5 * 11) - 2,
    'X371199',
    CURRENT_DATE - (5 * 11),
    CURRENT_DATE - (5 * 11) - 2,
    'e2543d2f-33a9-454b-ae15-03ca0475faa3',
    '6d6d4c56-9989-4fb5-a486-d32f525748e6',
    'temporary-accommodation',
    CURRENT_DATE,
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
    'f088b879-d6a4-4d7f-853b-e339074593cb',
    '541ab042-3b7b-4fb2-b1be-a36b42581ec3',
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
    '877542c1-b6ea-4f26-9a8e-587f07fea6fe',
    CURRENT_DATE - 1,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '541ab042-3b7b-4fb2-b1be-a36b42581ec3',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;

UPDATE arrivals SET arrival_date_time = cast(arrival_date as timestamp) at time zone 'utc';
