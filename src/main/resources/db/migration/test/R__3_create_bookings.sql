
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
    '864a4d2e-b034-40b1-9ace-136379fefa37',
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
    '75cf500b-3fe5-41a6-9169-6211aecdfbe6',
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
    'e19f51b8-1839-425f-ab96-263984589450',
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
    'f0494e25-9191-4ed9-8fbc-4b1929b7724a',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4Y29R9P',
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
    '5a90a5d5-396a-4258-bb21-58f89d628424',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'GSR1T2F',
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
    '26ea4c41-8110-4dd6-b0b2-63d478b832aa',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'HTVI42B',
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
    'a44b652e-c2bb-42d6-9f10-1a3e5590efdb',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'BWEFOI7',
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
    '9497f145-676d-49fc-978d-fa8091f3f162',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4Y29R9P',
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
    '82200e77-1c33-4872-9a3e-3b144a1d480a',
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
    'af3ad1be-8165-4109-9f6e-498d520524ab',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    '5EC66UT',
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
    '46babc4d-8a56-4ab5-b1d9-dd79331a340d',
    CURRENT_DATE + 1,
    CURRENT_DATE + 84,
    'BWEFOI7',
    CURRENT_DATE + 1,
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
    '2baa7fee-ea9b-47af-a2b1-28aaf221aeac',
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
    'a5f8e6d7-617c-4bec-a712-48c49168e773',
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
    'a5f8e6d7-617c-4bec-a712-48c49168e773',
    CURRENT_DATE,
    CURRENT_DATE,
    '44b28f26-8cd0-4932-b308-a90b2f4842cb',
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
    '6ed084a4-4f5a-4022-a9e7-2a150bc3a466',
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
    '6ed084a4-4f5a-4022-a9e7-2a150bc3a466',
    CURRENT_DATE,
    CURRENT_DATE,
    '5b1a2402-67dc-4032-9e84-df79c9667389',
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
    '5f9db685-3624-477b-960c-df0ce93c3d2e',
    CURRENT_DATE - 84,
    CURRENT_DATE + 1,
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE + 1,
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
    '5f9db685-3624-477b-960c-df0ce93c3d2e',
    CURRENT_DATE,
    CURRENT_DATE + 1,
    'e1354bb5-3993-4f8d-a500-33113755d88c',
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
    '394d666e-0c7e-41c9-9e46-4c1f572f6ac6',
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
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
    '394d666e-0c7e-41c9-9e46-4c1f572f6ac6',
    CURRENT_DATE,
    CURRENT_DATE + 3,
    'abb3ef0c-0210-4e18-ae59-944a550fc4a6',
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
    '77cde0b2-f32c-4f94-90b8-f5a08a2da292',
    CURRENT_DATE - 7,
    CURRENT_DATE + 47,
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 47,
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
    '77cde0b2-f32c-4f94-90b8-f5a08a2da292',
    CURRENT_DATE,
    CURRENT_DATE + 47,
    'e7475719-867b-4078-89bd-648334bb05c8',
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
    '45037922-ee06-4a3a-8825-e9144bb93473',
    CURRENT_DATE - 7,
    CURRENT_DATE + 10,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 10,
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
    '45037922-ee06-4a3a-8825-e9144bb93473',
    CURRENT_DATE,
    CURRENT_DATE + 10,
    'b5eaf22f-92f2-4123-9fb6-6d22f0662a45',
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
    '2e0305f1-93b7-4c0f-9afb-5232d1afe548',
    CURRENT_DATE - 7,
    CURRENT_DATE + 14,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 14,
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
    '2e0305f1-93b7-4c0f-9afb-5232d1afe548',
    CURRENT_DATE,
    CURRENT_DATE + 14,
    '9b6f047d-bcab-4492-8188-64cbd2a29c05',
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
    'db6c88bd-4eba-4d91-8a2d-d0fc12a8bba7',
    CURRENT_DATE - 7,
    CURRENT_DATE + 11,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 11,
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
    'db6c88bd-4eba-4d91-8a2d-d0fc12a8bba7',
    CURRENT_DATE,
    CURRENT_DATE + 11,
    '178e6c69-4072-4692-9f5d-b3525d28d22e',
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
    'c38d9049-6380-4b43-bde2-9d9451e65325',
    CURRENT_DATE - 7,
    CURRENT_DATE + 56,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 56,
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
    'c38d9049-6380-4b43-bde2-9d9451e65325',
    CURRENT_DATE,
    CURRENT_DATE + 56,
    '65105397-e5e1-4333-95c2-049d1a643469',
    NULL
  );
  
  
