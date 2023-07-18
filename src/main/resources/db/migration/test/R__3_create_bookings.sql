
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
    '5d5f5d8f-a1f2-4011-9033-31110855966b',
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
    '36857c66-c06d-404f-ada9-2edbf8828ae1',
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
    '81c6a11e-0c71-4610-835c-3be958c7fdc9',
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
    '9e52ec2a-f7ca-450f-ba7d-0bd0b3c988ae',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'N6OUTAY',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'fe86a602-6873-49d3-ac3a-3dfef743ae03',
    'temporary-accommodation',
    CURRENT_DATE,
   '6X1DNW1'
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
    '842ff087-9c64-47bd-8cd9-5069fcc964a0',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'QA93YYK',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
    'temporary-accommodation',
    CURRENT_DATE,
   'HIR0PIN'
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
    '36dd373b-33e8-444c-ac8b-5b67674159d9',
    '842ff087-9c64-47bd-8cd9-5069fcc964a0',
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
    '7169ba8a-2b5a-44c8-9729-55041d9a1c65',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
    'temporary-accommodation',
    CURRENT_DATE,
   'XWU5JWQ'
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
    '7169ba8a-2b5a-44c8-9729-55041d9a1c65',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '10abb045-112c-4d00-ae3c-65e96ad652ec',
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
    'c080d4a8-b2b4-41b9-9494-32f2c531c672',
    '7169ba8a-2b5a-44c8-9729-55041d9a1c65',
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
    'e0fc6cf2-8909-426a-adcf-eb3812255a3d',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4ZUIHFX',
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
    '24ecbe8d-71b0-481c-936d-b0c41f609ec3',
    CURRENT_DATE + 84,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    'e0fc6cf2-8909-426a-adcf-eb3812255a3d',
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
    'e0fc6cf2-8909-426a-adcf-eb3812255a3d',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '2847a51c-cf0f-4f0b-accc-b3fc5e27def8',
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
    '67acac14-09c1-42f2-b4e8-3847b48542f0',
    'e0fc6cf2-8909-426a-adcf-eb3812255a3d',
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
    '41439378-a366-4a3f-a0b0-68ae514be8b4',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'PI251LM',
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
    'cfe9a46a-c8ef-48b8-af17-1d6f16c01afe',
    CURRENT_DATE - 14,
    NULL,
    '41439378-a366-4a3f-a0b0-68ae514be8b4',
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
    '1e09a600-7fed-40c0-81b7-66d4399ff0db',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'HUN3BN0',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'bdf9f9f6-6d53-4577-bffe-fc5f0ab3de0f',
    'temporary-accommodation',
    CURRENT_DATE,
   'N1RRJZU'
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
    'be9cf40c-bbf4-4ed4-8a1d-ce5eeb69b5cf',
    CURRENT_DATE + 2,
    NULL,
    '1e09a600-7fed-40c0-81b7-66d4399ff0db',
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
    'dd6d3d5e-d7b5-4eb8-ab5c-b678916384fd',
    '1e09a600-7fed-40c0-81b7-66d4399ff0db',
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
    '08689e01-9d93-48f6-bffe-910688c5c3bc',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    '52W7TQG',
    CURRENT_DATE + 2,
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
    '913716e1-b4d1-4fe9-86aa-a50328e36799',
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
    '2b428837-c72e-4df2-940e-3be51176235e',
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
    '030a8ce8-f8d8-4709-9bd6-9166dbaee8fd',
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
    '1b723c34-51c2-4c9d-a96c-a29df81f2b71',
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
    '1b723c34-51c2-4c9d-a96c-a29df81f2b71',
    CURRENT_DATE,
    CURRENT_DATE,
    '44e56f1d-92d1-4689-8f9a-e8c66f528853',
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
    'de25bf21-dc7a-4bb0-a2ef-b55a2d4be78d',
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
    'de25bf21-dc7a-4bb0-a2ef-b55a2d4be78d',
    CURRENT_DATE,
    CURRENT_DATE,
    'd0f18137-b23c-42f9-acd5-7e2679db2c23',
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
    'e8a99da9-e21d-4839-bc28-38754bdcc359',
    CURRENT_DATE - 84,
    CURRENT_DATE + 4,
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE + 4,
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
    'e8a99da9-e21d-4839-bc28-38754bdcc359',
    CURRENT_DATE,
    CURRENT_DATE + 4,
    '3490a001-6012-4fa2-85fe-e07f3f904248',
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
    'ee478e6f-faa6-4d9f-b9d9-b427e508c000',
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
    'ee478e6f-faa6-4d9f-b9d9-b427e508c000',
    CURRENT_DATE,
    CURRENT_DATE + 4,
    'd5be0235-e16e-49a9-8a99-2dcffcfa59bd',
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
    '26c28f9c-df5f-4b48-92e0-7aaf93edabf3',
    CURRENT_DATE - 7,
    CURRENT_DATE + 33,
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 33,
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
    '26c28f9c-df5f-4b48-92e0-7aaf93edabf3',
    CURRENT_DATE,
    CURRENT_DATE + 33,
    'f73e0988-3357-4b67-a45a-3b688016109d',
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
    '86a751f5-30f8-4940-a8e8-02f41d0653aa',
    CURRENT_DATE - 7,
    CURRENT_DATE + 12,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 12,
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
    '86a751f5-30f8-4940-a8e8-02f41d0653aa',
    CURRENT_DATE,
    CURRENT_DATE + 12,
    'c69947bc-e3c1-41da-8608-530b2d6bffed',
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
    '02b2d503-912b-4f8a-8873-a76141154393',
    CURRENT_DATE - 7,
    CURRENT_DATE + 48,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 48,
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
    '02b2d503-912b-4f8a-8873-a76141154393',
    CURRENT_DATE,
    CURRENT_DATE + 48,
    '9bf8e777-e83d-45fc-9cc4-fd7046870360',
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
    '043c2ad6-aec2-4be2-ae83-b564be9c97cf',
    CURRENT_DATE - 7,
    CURRENT_DATE + 38,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 38,
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
    '043c2ad6-aec2-4be2-ae83-b564be9c97cf',
    CURRENT_DATE,
    CURRENT_DATE + 38,
    'c1435028-2dea-4ab8-89f0-5e8b7ca12759',
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
    '036421b4-a3f8-4897-b7c9-a5b66f8c7861',
    CURRENT_DATE - 7,
    CURRENT_DATE + 52,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 52,
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
    '036421b4-a3f8-4897-b7c9-a5b66f8c7861',
    CURRENT_DATE,
    CURRENT_DATE + 52,
    '05743e84-a3d6-40e8-9e8a-1c1b5ba91ddc',
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  
  
