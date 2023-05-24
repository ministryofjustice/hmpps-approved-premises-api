
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
    'c2fac91e-ebd1-4385-9706-2dfa6d5ddbf3',
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
    '30aa1edb-91f0-495d-90ac-7e203208e85b',
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
    '144bdd22-d5ae-4f6c-898f-c2bc3850bcef',
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
    'dca7487d-aea0-4722-ba3d-72b7f45b9187',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'PR5E5Y2',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'fe86a602-6873-49d3-ac3a-3dfef743ae03',
    'temporary-accommodation',
    CURRENT_DATE,
   'HBVE0LJ'
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
    'a4aa8a84-1d2e-4164-a179-fcbfe13d0802',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'Z33A1BU',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
    'temporary-accommodation',
    CURRENT_DATE,
   'M9XWJ1S'
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
    '1762b7c9-59d8-4731-b238-a184c05d0e9c',
    'a4aa8a84-1d2e-4164-a179-fcbfe13d0802',
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
    '3059fd6e-fdab-4580-8cb3-f934c31c580e',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'PR5E5Y2',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
    'temporary-accommodation',
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
    CURRENT_DATE,
    '3059fd6e-fdab-4580-8cb3-f934c31c580e',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'de4ce219-33a7-488e-9a3a-cad70c82d8cc',
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
    '40c1aa8a-d70d-43c7-96c5-6cae4f6223c6',
    '3059fd6e-fdab-4580-8cb3-f934c31c580e',
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
    '2bac3a05-ca83-4beb-a9ba-72e3ac717f84',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'N6OUTAY',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'd97bdcb9-f7b3-477b-a073-71939fac297a',
    'temporary-accommodation',
    CURRENT_DATE,
   'M9XWJ1S'
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
    'c790c4de-d666-4766-8e95-910c85df82d0',
    CURRENT_DATE + 84,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '2bac3a05-ca83-4beb-a9ba-72e3ac717f84',
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
    '2bac3a05-ca83-4beb-a9ba-72e3ac717f84',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '3b72ed4b-f451-4e66-acf4-10c6a1fbd066',
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
    '5943907b-d027-4758-a0e0-8a06ac133d09',
    '2bac3a05-ca83-4beb-a9ba-72e3ac717f84',
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
    '9fa491d7-d3b7-4cd5-a28b-3b99e22cb382',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'PR5E5Y2',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '8be1ed0e-dae7-42d2-97e0-95c95fdb4c50',
    'temporary-accommodation',
    CURRENT_DATE,
   '530X5EC'
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
    'c3439d8e-0129-407f-943e-9b85e9e678ab',
    CURRENT_DATE - 14,
    NULL,
    '9fa491d7-d3b7-4cd5-a28b-3b99e22cb382',
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
    'ab210bba-2fae-4026-b95a-9047be86f434',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'bdf9f9f6-6d53-4577-bffe-fc5f0ab3de0f',
    'temporary-accommodation',
    CURRENT_DATE,
   '530X5EC'
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
    '86a3be96-8698-43f3-9233-7b025630b07a',
    CURRENT_DATE + 2,
    NULL,
    'ab210bba-2fae-4026-b95a-9047be86f434',
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
    'f7ab4ff7-a255-4128-8ba3-cb4cd17d7115',
    'ab210bba-2fae-4026-b95a-9047be86f434',
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
    '527f41dd-3f09-460c-a983-f1017d15c5da',
    CURRENT_DATE + 3,
    CURRENT_DATE + 84,
    '52W7TQG',
    CURRENT_DATE + 3,
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
    'fb29625c-1eba-4625-8837-9427be371ad6',
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
    'c7ede9ab-9dce-4cdb-b8f0-044b4bfbca28',
    CURRENT_DATE + 1,
    CURRENT_DATE + 84,
    'BWEFOI7',
    CURRENT_DATE + 1,
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
    'd17a81a4-a647-48c3-ae1c-9fc41f9c9bb9',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE + 4,
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
    '18dce513-f47c-4650-ab6b-8a29fcdfce3e',
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
    '18dce513-f47c-4650-ab6b-8a29fcdfce3e',
    CURRENT_DATE,
    CURRENT_DATE,
    '74746a02-a2e9-4113-bc4c-616941704385',
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
    'f69f89e8-6c13-4fd8-bbe7-46a6ee6f5479',
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
    'f69f89e8-6c13-4fd8-bbe7-46a6ee6f5479',
    CURRENT_DATE,
    CURRENT_DATE,
    'b905285b-0af8-469b-81e8-ada9adf6665d',
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
    '80bd2d37-e552-41b5-825a-a0ec00b82cfa',
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
    '80bd2d37-e552-41b5-825a-a0ec00b82cfa',
    CURRENT_DATE,
    CURRENT_DATE + 3,
    '6f47be3e-e1af-4bf5-8b71-d2b6609a2a84',
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
    '155d4358-d852-4020-8208-68ee094145f9',
    CURRENT_DATE - 84,
    CURRENT_DATE + 2,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE + 2,
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
    '155d4358-d852-4020-8208-68ee094145f9',
    CURRENT_DATE,
    CURRENT_DATE + 2,
    'c4dea1f4-aa81-4f21-bb2f-216ca21bbc6d',
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
    'dc1cc901-344a-4401-9d31-6383393b19db',
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
    'dc1cc901-344a-4401-9d31-6383393b19db',
    CURRENT_DATE,
    CURRENT_DATE + 51,
    '125f2da8-2e23-4626-9c74-34040e43e813',
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
    '9732a2fb-9cc8-4cf1-91e8-b7ba4374f9dd',
    CURRENT_DATE - 7,
    CURRENT_DATE + 43,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 43,
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
    '9732a2fb-9cc8-4cf1-91e8-b7ba4374f9dd',
    CURRENT_DATE,
    CURRENT_DATE + 43,
    'ed055ade-ec66-4104-bbce-5955ee90893d',
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
    'b9e70ef9-1a83-4656-a6ff-cd091ebd8041',
    CURRENT_DATE - 7,
    CURRENT_DATE + 41,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 41,
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
    'b9e70ef9-1a83-4656-a6ff-cd091ebd8041',
    CURRENT_DATE,
    CURRENT_DATE + 41,
    '037d9868-65ef-4b8b-9720-63542bd259ac',
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
    '097d8fc9-387b-4c35-9e4e-3935ebfeb752',
    CURRENT_DATE - 7,
    CURRENT_DATE + 19,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 19,
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
    '097d8fc9-387b-4c35-9e4e-3935ebfeb752',
    CURRENT_DATE,
    CURRENT_DATE + 19,
    '43ea70f7-abaf-4850-b652-c828e48dde26',
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
    '002caec5-145e-4fb4-a2df-0805d34c664a',
    CURRENT_DATE - 7,
    CURRENT_DATE + 8,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 8,
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
    '002caec5-145e-4fb4-a2df-0805d34c664a',
    CURRENT_DATE,
    CURRENT_DATE + 8,
    '704b4d8a-f6e1-4d7d-8f8c-1faacf813572',
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  
  
