
-- ${flyway:timestamp}
--- Add some Bookings arriving today ---

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
    '3f163bc3-5920-4c26-add2-6eb4049bfb79',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '315VWWC',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'M59CS58'
  )
ON CONFLICT(id) DO NOTHING;


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
    '9a212ec1-0103-4ef6-ae22-bf2a7fe79ea5',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4Y29R9P',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'XWU5JWQ'
  )
ON CONFLICT(id) DO NOTHING;


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
    'd41a59e3-41c3-4682-bb52-c8c06c5eb38c',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4ZUIHFX',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   '5A5C8WL'
  )
ON CONFLICT(id) DO NOTHING;

--- Add some Temporary accommodation bookings ---

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
    'b657008d-cf60-4bc4-b0d6-ce7e85ecd2f7',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'HUN3BN0',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'fe86a602-6873-49d3-ac3a-3dfef743ae03',
    'temporary-accommodation',
    CURRENT_DATE,
   'M9XWJ1S'
  )
ON CONFLICT(id) DO NOTHING;


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
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '315VWWC',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
    'temporary-accommodation',
    CURRENT_DATE,
   'VTDINBA'
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
    '832af233-482e-4060-b05d-b0db86c05976',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'XCMSG3I',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
    'temporary-accommodation',
    CURRENT_DATE,
   'KXTJQEF'
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
    CURRENT_DATE,
    '832af233-482e-4060-b05d-b0db86c05976',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '104522d0-8c41-4222-9905-0c32e1ab593e',
    NULL
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
    'bccd37d1-803a-4f5a-b8df-d7c623bdcdab',
    '832af233-482e-4060-b05d-b0db86c05976',
    CURRENT_DATE,
    NULL,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;
  

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
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'XCMSG3I',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'd97bdcb9-f7b3-477b-a073-71939fac297a',
    'temporary-accommodation',
    CURRENT_DATE,
   '5A5C8WL'
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
    CURRENT_DATE + 84,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    'e3a08fac-88b3-4691-ad6f-b26b9180b1c6',
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
    CURRENT_DATE,
    'e3a08fac-88b3-4691-ad6f-b26b9180b1c6',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '18721709-f4f1-46ed-b9eb-4cb04f3711c9',
    NULL
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
    '25efa393-fd03-4ed6-a4a0-5f9bbb0c49d0',
    'e3a08fac-88b3-4691-ad6f-b26b9180b1c6',
    CURRENT_DATE,
    NULL,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;
  

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
    '3d010f87-19a3-4094-89b3-9ecbf32332aa',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4ZUIHFX',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '8be1ed0e-dae7-42d2-97e0-95c95fdb4c50',
    'temporary-accommodation',
    CURRENT_DATE,
   '6X1DNW1'
  )
ON CONFLICT(id) DO NOTHING;


INSERT INTO
  cancellations (
    "id",
    "date",
    "notes",
    "booking_id",
    "cancellation_reason_id",
    "created_at"
  )
VALUES
  (
    'e2b00fc3-2c17-40ef-a25d-ab3cceaba905',
    CURRENT_DATE - 14,
    NULL,
    '3d010f87-19a3-4094-89b3-9ecbf32332aa',
    'd2a0d037-53db-4bb2-b9f7-afa07948a3f5',
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;
  

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
    '0418ceff-7234-4606-afc6-95e1721dac35',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'HRV83TE',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'bdf9f9f6-6d53-4577-bffe-fc5f0ab3de0f',
    'temporary-accommodation',
    CURRENT_DATE,
   'M59CS58'
  )
ON CONFLICT(id) DO NOTHING;


INSERT INTO
  non_arrivals (
    "id",
    "date",
    "notes",
    "booking_id",
    "non_arrival_reason_id",
    "created_at"
  )
VALUES
  (
    '536e739e-1410-460e-bfae-9171f78c7ec7',
    CURRENT_DATE + 2,
    NULL,
    '0418ceff-7234-4606-afc6-95e1721dac35',
    'e9184f2e-f409-461e-b149-492a02cb1655',
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
    '8ed78217-c2a2-48ac-b248-fc0bca8f0d6e',
    '0418ceff-7234-4606-afc6-95e1721dac35',
    CURRENT_DATE,
    NULL,
    CURRENT_DATE
  )
ON CONFLICT(id) DO NOTHING;
  
--- Add some Bookings arriving soon ---

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
    '52W7TQG',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   '5HBWEG1'
  )
ON CONFLICT(id) DO NOTHING;


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
    'e17a178f-9e9c-4916-9407-1dd47480f3dc',
    CURRENT_DATE + 1,
    CURRENT_DATE + 84,
    '5EC66UT',
    CURRENT_DATE + 1,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   '530X5EC'
  )
ON CONFLICT(id) DO NOTHING;


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
    '151cf62a-00e2-41c4-a8fc-0e500c22ec62',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    'BWEFOI7',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'SX9UI94'
  )
ON CONFLICT(id) DO NOTHING;


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
    'c1e613eb-2560-43ae-a340-9d9eec602369',
    CURRENT_DATE + 1,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE + 1,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'KXTJQEF'
  )
ON CONFLICT(id) DO NOTHING;

--- Add some Bookings departing today ---

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
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'N1RRJZU'
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
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  

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
    '211d8d4b-3cd3-40b4-b770-090482e85f26',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   '6X1DNW1'
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
    '211d8d4b-3cd3-40b4-b770-090482e85f26',
    CURRENT_DATE,
    CURRENT_DATE,
    'ed0fc817-71cb-45ab-958f-862676fa83f0',
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  
--- Add some Bookings departing soon ---

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
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'N1RRJZU'
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
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  

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
    '8961aa8e-49c7-4de7-8355-822e99486b95',
    CURRENT_DATE - 84,
    CURRENT_DATE + 4,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE + 4,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   '6X1DNW1'
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
    '8961aa8e-49c7-4de7-8355-822e99486b95',
    CURRENT_DATE,
    CURRENT_DATE + 4,
    '8f40bc7f-75a2-4150-b7df-e602acfc93e5',
    NULL
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
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 51,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'YCSW8BD'
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
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  

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
    '05137bee-650c-4d3f-ba40-65b873e67d67',
    CURRENT_DATE - 7,
    CURRENT_DATE + 55,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 55,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'KWAPTC7'
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
    '05137bee-650c-4d3f-ba40-65b873e67d67',
    CURRENT_DATE,
    CURRENT_DATE + 55,
    'e859bc61-3d04-4fd5-9bbc-240be8c2aebe',
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  

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
    'c5001b59-deeb-4a79-ad44-8d3a4d67c3d9',
    CURRENT_DATE - 7,
    CURRENT_DATE + 50,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 50,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'VTDINBA'
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
    'c5001b59-deeb-4a79-ad44-8d3a4d67c3d9',
    CURRENT_DATE,
    CURRENT_DATE + 50,
    '28cea0f4-b055-45c9-a5fe-762112417bb2',
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  

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
    '3880f4c7-9c72-4c3d-806e-5d32bde19abc',
    CURRENT_DATE - 7,
    CURRENT_DATE + 29,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 29,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'HIR0PIN'
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
    '3880f4c7-9c72-4c3d-806e-5d32bde19abc',
    CURRENT_DATE,
    CURRENT_DATE + 29,
    'd45bea8e-3c82-4502-b30a-9b9c13dc1e6b',
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  

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
    'd31f6e77-7612-445c-9b0d-598a6fcec1b0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 28,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 28,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE,
   'M9XWJ1S'
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
    'd31f6e77-7612-445c-9b0d-598a6fcec1b0',
    CURRENT_DATE,
    CURRENT_DATE + 28,
    '3a801c1b-6bef-4697-9aa2-2d2de5ffec50',
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  
  
