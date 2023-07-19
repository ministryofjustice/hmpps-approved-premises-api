
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
    'def84098-99b2-45a9-a075-36759f2d6b59',
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
    '422ee4ce-4ae3-4459-871d-f95dd37bc739',
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
    '78355e98-6907-44a6-a06c-86bdb8d66fa4',
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
    '62991f82-0d04-411a-9128-4af4cb557989',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'Z33A1BU',
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
    'a36d16c0-47d8-4aad-b9c9-f7cf6c2c4910',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4Y29R9P',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
    'temporary-accommodation',
    CURRENT_DATE,
   'JD5CLIA'
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
    '3cc931d8-05c3-49e5-99df-2eb9948ce903',
    'a36d16c0-47d8-4aad-b9c9-f7cf6c2c4910',
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
    '781f468b-4057-49e2-86cf-812db2fccb98',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'PI251LM',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
    'temporary-accommodation',
    CURRENT_DATE,
   'QGH3OL6'
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
    '781f468b-4057-49e2-86cf-812db2fccb98',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'f78ce875-fb12-4bd8-805d-1f756af7e2cf',
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
    'e8693c79-ea5a-4b45-9699-8a4196cf3bb5',
    '781f468b-4057-49e2-86cf-812db2fccb98',
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
    '8e3fe693-8dea-4298-a063-c982b605109f',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'd97bdcb9-f7b3-477b-a073-71939fac297a',
    'temporary-accommodation',
    CURRENT_DATE,
   '6X1DNW1'
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
    '44b03c96-e9c7-46be-87c6-da83d1286725',
    CURRENT_DATE + 84,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '8e3fe693-8dea-4298-a063-c982b605109f',
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
    '8e3fe693-8dea-4298-a063-c982b605109f',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '9d60af18-0f3c-49fd-b170-ab5380645025',
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
    '0785ad91-3bcd-4ba8-9cbe-1d6f6c3beb8e',
    '8e3fe693-8dea-4298-a063-c982b605109f',
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
    '9dfa0826-88bd-45ac-8c45-9f545d2e4175',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'QA93YYK',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '8be1ed0e-dae7-42d2-97e0-95c95fdb4c50',
    'temporary-accommodation',
    CURRENT_DATE,
   'M59CS58'
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
    'e9f03ab0-c128-4a9d-9036-7f09326ae8f7',
    CURRENT_DATE - 14,
    NULL,
    '9dfa0826-88bd-45ac-8c45-9f545d2e4175',
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
    'd56d10b0-82f9-4f07-b23f-0571d46ba89d',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'PI251LM',
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
    '8b869ea7-f6e0-471b-9017-429332558ef9',
    CURRENT_DATE + 2,
    NULL,
    'd56d10b0-82f9-4f07-b23f-0571d46ba89d',
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
    '44620f37-9af3-42f0-a000-8a11ebc02d1b',
    'd56d10b0-82f9-4f07-b23f-0571d46ba89d',
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
    '49c1e823-9390-419f-a53a-7b362ae3380a',
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
    '939ee5a3-4291-4ce9-8411-872ddb85bfc8',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '5EC66UT',
    CURRENT_DATE + 4,
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
    'efe873da-49d1-4abd-97d9-2d5e12ef93c2',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    'BWEFOI7',
    CURRENT_DATE + 2,
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
    '843131e2-b0b0-406f-a8a0-95db48e0c3c8',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE + 2,
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
    '8f5f08b8-ef43-4aa2-8d0d-12eb0b7752ee',
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
    '8f5f08b8-ef43-4aa2-8d0d-12eb0b7752ee',
    CURRENT_DATE,
    CURRENT_DATE,
    '786a9eb1-f624-4a15-ac37-57da05ed2de9',
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
    '0c886e23-cf31-46cb-891d-fb2437d5dcfc',
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
    '0c886e23-cf31-46cb-891d-fb2437d5dcfc',
    CURRENT_DATE,
    CURRENT_DATE,
    '258f566b-94d4-4d65-a0dd-7b4ef450a77e',
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
    '217ead16-3e5a-412f-b0bf-82eb5ec1248b',
    CURRENT_DATE - 84,
    CURRENT_DATE + 2,
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE + 2,
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
    '217ead16-3e5a-412f-b0bf-82eb5ec1248b',
    CURRENT_DATE,
    CURRENT_DATE + 2,
    '89c345d4-86de-4b71-b0bf-1a67bd2782b4',
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
    '08d248bb-c459-448a-ad0b-8a4bd526a116',
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
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
    '08d248bb-c459-448a-ad0b-8a4bd526a116',
    CURRENT_DATE,
    CURRENT_DATE + 3,
    '79fbef04-7609-4bb9-82f7-810b1f101ce8',
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
    '9e028c82-33cb-441f-8780-39e52695f764',
    CURRENT_DATE - 7,
    CURRENT_DATE + 24,
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 24,
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
    '9e028c82-33cb-441f-8780-39e52695f764',
    CURRENT_DATE,
    CURRENT_DATE + 24,
    '425f203c-e9cb-468c-a313-fc25c9ba79fe',
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
    'b80c1f22-f43a-486b-90ba-b486c3c558b1',
    CURRENT_DATE - 7,
    CURRENT_DATE + 14,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 14,
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
    'b80c1f22-f43a-486b-90ba-b486c3c558b1',
    CURRENT_DATE,
    CURRENT_DATE + 14,
    'd66b0a7d-1341-4a0b-9374-f886b7ca6076',
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
    '3e4ad184-82fe-4a05-af43-60db6c4d2307',
    CURRENT_DATE - 7,
    CURRENT_DATE + 22,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 22,
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
    '3e4ad184-82fe-4a05-af43-60db6c4d2307',
    CURRENT_DATE,
    CURRENT_DATE + 22,
    '42ee4eeb-291f-408d-88e3-41807b84b0d7',
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
    '9e2d17cb-f863-4a75-9238-529b3f480381',
    CURRENT_DATE - 7,
    CURRENT_DATE + 42,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 42,
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
    '9e2d17cb-f863-4a75-9238-529b3f480381',
    CURRENT_DATE,
    CURRENT_DATE + 42,
    '28bc6de3-1c7b-4721-907a-4a107a886b95',
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
    '4d552e03-5e97-4e1c-95c7-659d78f199f4',
    CURRENT_DATE - 7,
    CURRENT_DATE + 25,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 25,
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
    '4d552e03-5e97-4e1c-95c7-659d78f199f4',
    CURRENT_DATE,
    CURRENT_DATE + 25,
    '595a8e3a-4c6a-4be3-bba2-7d6d079cd438',
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  
  
