
-- ${flyway:timestamp}
TRUNCATE TABLE arrivals CASCADE;
TRUNCATE TABLE bookings CASCADE;
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
    "created_at"
  )
VALUES
  (
    'afcabcb9-7abe-495c-a67e-381380fbcc2d',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '315VWWC',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    "created_at"
  )
VALUES
  (
    '21b99116-6e7e-4c90-afa0-a1be915fb36c',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4Y29R9P',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    "created_at"
  )
VALUES
  (
    '480a0b60-e486-4333-aad0-4e33432a7081',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4ZUIHFX',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );

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
    "created_at"
  )
VALUES
  (
    '29fe043d-2a15-42f7-9a1e-fdeca535e893',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'QA93YYK',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'fe86a602-6873-49d3-ac3a-3dfef743ae03',
    'temporary-accommodation',
    CURRENT_DATE
  );


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
    "created_at"
  )
VALUES
  (
    '963d2738-2420-4595-83d5-0b11aa2e4a06',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '315VWWC',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
    'temporary-accommodation',
    CURRENT_DATE
  );


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
    "created_at"
  )
VALUES
  (
    '7e56bed8-8675-4359-8b74-96dd76af599c',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '52W7TQG',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '135812b4-e6c0-4ccf-9502-4bfea66f3bd3',
    'temporary-accommodation',
    CURRENT_DATE
  );


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
    "created_at"
  )
VALUES
  (
    'fcbec43a-8186-43d0-bfb2-f607e6126ffc',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '5EC66UT',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'd97bdcb9-f7b3-477b-a073-71939fac297a',
    'temporary-accommodation',
    CURRENT_DATE
  );


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
    "created_at"
  )
VALUES
  (
    '3c2e85f6-6fde-4417-91ec-c343d66d0287',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'Z33A1BU',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '8be1ed0e-dae7-42d2-97e0-95c95fdb4c50',
    'temporary-accommodation',
    CURRENT_DATE
  );

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
    "created_at"
  )
VALUES
  (
    'a5f250a6-bdf1-4cdd-b0e7-ceeb888c934d',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '52W7TQG',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    "created_at"
  )
VALUES
  (
    'b1e57e3a-254c-452a-846e-681911718789',
    CURRENT_DATE + 3,
    CURRENT_DATE + 84,
    '5EC66UT',
    CURRENT_DATE + 3,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    "created_at"
  )
VALUES
  (
    '13e80c7e-05f4-4582-9737-afee19f286ef',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    'BWEFOI7',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    "created_at"
  )
VALUES
  (
    '8431d276-a7a8-4836-96d6-9d7cf2625ae4',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );

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
    "created_at"
  )
VALUES
  (
    '16e7f3fb-5353-42fe-b3c1-e6874733d899',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    '16e7f3fb-5353-42fe-b3c1-e6874733d899',
    CURRENT_DATE,
    CURRENT_DATE,
    'e9543cc9-a253-422a-8a61-0b14de8d1bcc',
    NULL
  );
  

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
    "created_at"
  )
VALUES
  (
    'aad04043-9702-474c-a617-bcdf18729d3e',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    'aad04043-9702-474c-a617-bcdf18729d3e',
    CURRENT_DATE,
    CURRENT_DATE,
    '2d6e14c7-1c04-4f8e-aec6-e5910a68f33d',
    NULL
  );
  
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
    "created_at"
  )
VALUES
  (
    '26844b1a-d847-4c5b-b8cc-1e3e50c04438',
    CURRENT_DATE - 84,
    CURRENT_DATE + 4,
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE + 4,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    '26844b1a-d847-4c5b-b8cc-1e3e50c04438',
    CURRENT_DATE,
    CURRENT_DATE + 4,
    'db3ef90d-7c78-46bc-a3b4-e80b3b326b48',
    NULL
  );
  

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
    "created_at"
  )
VALUES
  (
    'c060600b-0e5b-475f-acfd-74a79a7bbd7e',
    CURRENT_DATE - 84,
    CURRENT_DATE + 4,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE + 4,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    'c060600b-0e5b-475f-acfd-74a79a7bbd7e',
    CURRENT_DATE,
    CURRENT_DATE + 4,
    'dc43f4e0-5e09-44af-a7a3-a5482122a6cb',
    NULL
  );
  
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
    "created_at"
  )
VALUES
  (
    '78ca5657-1797-46bd-9fc3-3cec04fd8263',
    CURRENT_DATE - 7,
    CURRENT_DATE + 48,
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 48,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    '78ca5657-1797-46bd-9fc3-3cec04fd8263',
    CURRENT_DATE,
    CURRENT_DATE + 48,
    'f15cb65e-79f1-408e-8764-fbe71e9e709f',
    NULL
  );
  

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
    "created_at"
  )
VALUES
  (
    '76b32d8e-1913-4c05-9433-a22f0357a3a4',
    CURRENT_DATE - 7,
    CURRENT_DATE + 25,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 25,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    '76b32d8e-1913-4c05-9433-a22f0357a3a4',
    CURRENT_DATE,
    CURRENT_DATE + 25,
    '819ebbf3-6f88-4b67-a7e9-8ecfe2b8a136',
    NULL
  );
  

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
    "created_at"
  )
VALUES
  (
    'f1b73388-93ec-4bcf-9d27-601099015621',
    CURRENT_DATE - 7,
    CURRENT_DATE + 8,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 8,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    'f1b73388-93ec-4bcf-9d27-601099015621',
    CURRENT_DATE,
    CURRENT_DATE + 8,
    '75507b9f-d95c-4ed6-baee-1368546a98e5',
    NULL
  );
  

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
    "created_at"
  )
VALUES
  (
    '42305942-8b3d-4e98-af0f-4ce0edd40693',
    CURRENT_DATE - 7,
    CURRENT_DATE + 60,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 60,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    '42305942-8b3d-4e98-af0f-4ce0edd40693',
    CURRENT_DATE,
    CURRENT_DATE + 60,
    '4f48352d-d671-4ddf-baef-dc7693dc4d42',
    NULL
  );
  

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
    "created_at"
  )
VALUES
  (
    '55aa555a-84a9-4aeb-a81d-bdc7a6888c74',
    CURRENT_DATE - 7,
    CURRENT_DATE + 52,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 52,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    NULL,
    'approved-premises',
    CURRENT_DATE
  );


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
    '55aa555a-84a9-4aeb-a81d-bdc7a6888c74',
    CURRENT_DATE,
    CURRENT_DATE + 52,
    '06f64391-0f46-4759-862c-d97b4cdeface',
    NULL
  );
  
  
