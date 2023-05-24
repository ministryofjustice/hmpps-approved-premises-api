
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
    '2f706405-df97-4e26-88bb-7901f528620f',
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
    '4a2e9711-12d0-432f-958d-2fbd76606e4c',
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
    '2a8dd273-7d5b-4b80-9a50-d720aa500726',
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
    'aca579ba-9c6f-46b8-8217-01efad42ab2a',
    CURRENT_DATE + 84,
    fe86a602-6873-49d3-ac3a-3dfef743ae03,
    '315VWWC',
    CURRENT_DATE + 84,
    fe86a602-6873-49d3-ac3a-3dfef743ae03,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '7012a949-a110-4800-b0f1-83e241612a0e',
    'temporary-accommodation',
    CURRENT_DATE,
   'CURRENT_DATE'
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
    '1a774d4b-b7d5-434f-a5cf-a1568a5f076c',
    CURRENT_DATE + 84,
    e8887df9-b31b-4e9c-931a-e063d778ab0d,
    'Z33A1BU',
    CURRENT_DATE + 84,
    e8887df9-b31b-4e9c-931a-e063d778ab0d,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'ff590a39-7c17-40ed-8960-8469a40f0052',
    'temporary-accommodation',
    CURRENT_DATE,
   'CURRENT_DATE'
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
    '7446498f-0e2e-4bad-98c5-11a8d591a2bd',
    'ff590a39-7c17-40ed-8960-8469a40f0052',
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
    'c2abbe16-7553-46a4-a78a-f450106ac4d7',
    CURRENT_DATE + 84,
    135812b4-e6c0-4ccf-9502-4bfea66f3bd3,
    'PR5E5Y2',
    CURRENT_DATE + 84,
    135812b4-e6c0-4ccf-9502-4bfea66f3bd3,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '1e0f0226-6aba-4f69-a12b-7c8ff7b02fe6',
    'temporary-accommodation',
    CURRENT_DATE,
   'CURRENT_DATE'
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
    '1e0f0226-6aba-4f69-a12b-7c8ff7b02fe6',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '390d7e4e-9e59-422c-aaac-70ff0e1b9322',
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
    'a67e9e1e-ce61-4375-aae8-7b3e87bf1717',
    '1e0f0226-6aba-4f69-a12b-7c8ff7b02fe6',
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
    '4a324a8a-de50-45c5-b0f6-0c57db8f6319',
    CURRENT_DATE + 84,
    d97bdcb9-f7b3-477b-a073-71939fac297a,
    'HRV83TE',
    CURRENT_DATE + 84,
    d97bdcb9-f7b3-477b-a073-71939fac297a,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '414cdfcb-aeba-435a-9b89-0a2fa54a97f1',
    'temporary-accommodation',
    CURRENT_DATE,
   'CURRENT_DATE'
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
    '38c68463-e67e-4744-b8e4-5e91b423fe1e',
    CURRENT_DATE + 84,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '414cdfcb-aeba-435a-9b89-0a2fa54a97f1',
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
    '414cdfcb-aeba-435a-9b89-0a2fa54a97f1',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '0043d69a-295c-4217-be59-bb51a8b70077',
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
    '3f976d16-67ec-4157-9061-8f3898e33f7f',
    '414cdfcb-aeba-435a-9b89-0a2fa54a97f1',
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
    'fb620235-b5b4-44a7-b9d6-32d670652313',
    CURRENT_DATE + 84,
    8be1ed0e-dae7-42d2-97e0-95c95fdb4c50,
    'QA93YYK',
    CURRENT_DATE + 84,
    8be1ed0e-dae7-42d2-97e0-95c95fdb4c50,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '22485508-28d4-4805-ab76-42ab50a61f3a',
    'temporary-accommodation',
    CURRENT_DATE,
   'CURRENT_DATE'
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
    '599f1025-da07-457c-bec6-c4fc3b84909d',
    CURRENT_DATE - 14,
    NULL,
    '22485508-28d4-4805-ab76-42ab50a61f3a',
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
    '9a4fd241-3219-469e-a06c-31fc6b52b732',
    CURRENT_DATE + 84,
    bdf9f9f6-6d53-4577-bffe-fc5f0ab3de0f,
    'YRPARSH',
    CURRENT_DATE + 84,
    bdf9f9f6-6d53-4577-bffe-fc5f0ab3de0f,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '57372b15-9bb9-4ba3-823d-9e4d6b70a5a2',
    'temporary-accommodation',
    CURRENT_DATE,
   'CURRENT_DATE'
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
    'ae202db4-4c47-4438-a7b8-82b218f0c129',
    CURRENT_DATE + 2,
    NULL,
    '57372b15-9bb9-4ba3-823d-9e4d6b70a5a2',
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
    'bfcc2f81-7761-404e-80c1-85d87eaaa494',
    '57372b15-9bb9-4ba3-823d-9e4d6b70a5a2',
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
    'd3e054cf-9cb1-4888-b22c-5c958931815e',
    CURRENT_DATE + 1,
    CURRENT_DATE + 84,
    '52W7TQG',
    CURRENT_DATE + 1,
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
    '22737215-4050-4c38-a696-34a720021fb6',
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
    '4cb056e5-ae5e-4844-b218-b4080232c098',
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
    '2268da8b-7334-4f52-858e-08acd49a4940',
    CURRENT_DATE + 3,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE + 3,
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
    '4ed1a01a-8acf-4581-9273-3c846090968d',
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
    '4ed1a01a-8acf-4581-9273-3c846090968d',
    CURRENT_DATE,
    CURRENT_DATE,
    'c485e9bb-ef14-4320-9036-6271cee397ef',
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
    '35e726ee-f457-4e1b-b65d-5813ea7513dc',
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
    '35e726ee-f457-4e1b-b65d-5813ea7513dc',
    CURRENT_DATE,
    CURRENT_DATE,
    'c1e9d4a7-9e24-4ac7-84af-b4272fdf87dd',
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
    '6ea7cc92-01c2-488a-ae5c-3e205e605e31',
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
    '6ea7cc92-01c2-488a-ae5c-3e205e605e31',
    CURRENT_DATE,
    CURRENT_DATE + 3,
    '35a76262-5fd8-4171-8bfe-522abcb9857f',
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
    '94bc95b9-3e21-4ef1-9650-35cdecac8cf0',
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
    '94bc95b9-3e21-4ef1-9650-35cdecac8cf0',
    CURRENT_DATE,
    CURRENT_DATE + 2,
    '7189f560-d3bc-46a0-bc89-0ea0fcd3ccc8',
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
    'd6b2dd4c-4213-4325-965c-0407c5b22119',
    CURRENT_DATE - 7,
    CURRENT_DATE + 7,
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 7,
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
    'd6b2dd4c-4213-4325-965c-0407c5b22119',
    CURRENT_DATE,
    CURRENT_DATE + 7,
    'b459665e-3cd6-4140-9563-bb6d8090f7f3',
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
    '50c204ab-b9c0-4c3e-8733-2ee033e2f56a',
    CURRENT_DATE - 7,
    CURRENT_DATE + 20,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 20,
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
    '50c204ab-b9c0-4c3e-8733-2ee033e2f56a',
    CURRENT_DATE,
    CURRENT_DATE + 20,
    'e452903e-70e9-4ab5-9f12-1a82c5de0395',
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
    'aa361efc-5073-412f-b8cf-bb6ca4549732',
    CURRENT_DATE - 7,
    CURRENT_DATE + 17,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 17,
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
    'aa361efc-5073-412f-b8cf-bb6ca4549732',
    CURRENT_DATE,
    CURRENT_DATE + 17,
    '4dcc9c7e-6c5c-4448-b355-0d80c829e5f2',
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
    'c81d8cbd-fdb4-408d-aff5-8f597be68c6a',
    CURRENT_DATE - 7,
    CURRENT_DATE + 21,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 21,
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
    'c81d8cbd-fdb4-408d-aff5-8f597be68c6a',
    CURRENT_DATE,
    CURRENT_DATE + 21,
    'b3de9bd5-a8da-4436-904c-9202c02dd9b0',
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
    'f322414a-33c9-43fc-8b3b-749915fc30ae',
    CURRENT_DATE - 7,
    CURRENT_DATE + 14,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 14,
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
    'f322414a-33c9-43fc-8b3b-749915fc30ae',
    CURRENT_DATE,
    CURRENT_DATE + 14,
    '4de1ae85-d6d1-4652-a551-a93635449d53',
    NULL
  )
ON CONFLICT(id) DO NOTHING;
  
  
