
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
    '85c8eb35-5c48-436b-9c1d-7791ba3abb1c',
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
    '0a3555d0-2aa2-4825-8e56-b759540ae0ce',
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
    'fa21630f-4b7f-452e-9a78-dbadd748bc17',
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
    '8067f115-d87e-4ed4-9242-53eacd749042',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'N6OUTAY',
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
    '3bab931b-b694-4651-99fc-83f50b030ad1',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '5EC66UT',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'e8887df9-b31b-4e9c-931a-e063d778ab0d',
    'temporary-accommodation',
    CURRENT_DATE
  );


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
    '96e87ea3-4116-4fcd-aff4-4c6bb3aaecf6',
    '3bab931b-b694-4651-99fc-83f50b030ad1',
    CURRENT_DATE,
    NULL,
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
    '20f6e044-78d9-4e0b-8098-ccb4926d51bd',
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
    '20f6e044-78d9-4e0b-8098-ccb4926d51bd',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'f6b4d21e-f7cc-4417-8f08-33cae23a8252',
    NULL
  );
  

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
    'f956f4cd-208c-41ae-b186-50afb1b9d857',
    '20f6e044-78d9-4e0b-8098-ccb4926d51bd',
    CURRENT_DATE,
    NULL,
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
    '12e35bda-00f7-4d12-bbc3-733ed9b4fda6',
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
    '8a30d8cb-3404-4db5-ae8f-54e3efdeb46b',
    CURRENT_DATE + 84,
    'f4d00e1c-8bfd-40e9-8241-a7d0f744e737',
    '587dc0dc-9073-4992-9d58-5576753050e9',
    NULL,
    NULL,
    '12e35bda-00f7-4d12-bbc3-733ed9b4fda6',
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
    CURRENT_DATE,
    '12e35bda-00f7-4d12-bbc3-733ed9b4fda6',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '11176e1c-48e2-4beb-a81e-4a8eb54a68d1',
    NULL
  );
  

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
    'bec85422-4590-4d6e-b5df-7205298790d5',
    '12e35bda-00f7-4d12-bbc3-733ed9b4fda6',
    CURRENT_DATE,
    NULL,
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
    'bfd363fc-b1be-480d-bc1f-4a928c99ab50',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'YRPARSH',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    '8be1ed0e-dae7-42d2-97e0-95c95fdb4c50',
    'temporary-accommodation',
    CURRENT_DATE
  );


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
    'b1a73c9a-20ad-4fcc-88a7-e255f8e43903',
    CURRENT_DATE - 14,
    NULL,
    'bfd363fc-b1be-480d-bc1f-4a928c99ab50',
    'd2a0d037-53db-4bb2-b9f7-afa07948a3f5',
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
    '241cfcac-c6c2-429f-9317-690fb6bfe4a5',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'JCRH9V5',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'd33006b7-55d9-4a8e-b722-5e18093dbcdf',
    'bdf9f9f6-6d53-4577-bffe-fc5f0ab3de0f',
    'temporary-accommodation',
    CURRENT_DATE
  );


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
    '0caefa7e-3618-4708-9307-cb6266538212',
    CURRENT_DATE + 2,
    NULL,
    '241cfcac-c6c2-429f-9317-690fb6bfe4a5',
    'e9184f2e-f409-461e-b149-492a02cb1655',
    CURRENT_DATE
  );
  

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
    '102c2e18-19d2-4fa7-bbf8-ea5d0628e62e',
    '241cfcac-c6c2-429f-9317-690fb6bfe4a5',
    CURRENT_DATE,
    NULL,
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
    'cf3003db-b146-4b1f-b5a8-5a7f9d6223c8',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    '52W7TQG',
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
    '6e5dcbed-7d52-496f-8958-6503f156fbff',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '5EC66UT',
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
    '164d7a18-7a56-4628-a95d-a8bff55a1f1f',
    CURRENT_DATE + 3,
    CURRENT_DATE + 84,
    'BWEFOI7',
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
    'fc943a82-2966-4a8c-92f7-94cc7f29c899',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE + 2,
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
    '65304b6a-61de-4930-a54f-5e1e3eb4b21d',
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
    '65304b6a-61de-4930-a54f-5e1e3eb4b21d',
    CURRENT_DATE,
    CURRENT_DATE,
    'b2af3908-5f3f-40ef-84cd-0df5683c0f05',
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
    '08b7c9ed-6900-4fa4-bd8f-7e431cfce5fd',
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
    '08b7c9ed-6900-4fa4-bd8f-7e431cfce5fd',
    CURRENT_DATE,
    CURRENT_DATE,
    'e20e1cd8-5581-41e9-9060-1c30bfc78d98',
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
    'ed29449d-fd5c-42bb-a685-676c9f8f8dec',
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
    'ed29449d-fd5c-42bb-a685-676c9f8f8dec',
    CURRENT_DATE,
    CURRENT_DATE + 1,
    'd8f0efab-07b4-48cf-a1f1-ad2b73eac279',
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
    'd19a5b81-2b76-4d5a-893e-52cdc1a601b8',
    CURRENT_DATE - 84,
    CURRENT_DATE + 2,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE + 2,
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
    'd19a5b81-2b76-4d5a-893e-52cdc1a601b8',
    CURRENT_DATE,
    CURRENT_DATE + 2,
    '06534dfa-0108-4dbf-9ebc-6f8141bfd31e',
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
    'c64f0e8e-c8a7-41ac-9bf3-d58b28d527b7',
    CURRENT_DATE - 7,
    CURRENT_DATE + 13,
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 13,
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
    'c64f0e8e-c8a7-41ac-9bf3-d58b28d527b7',
    CURRENT_DATE,
    CURRENT_DATE + 13,
    'ee52f4ef-22b7-49d3-9931-112c98ce9457',
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
    '04206c50-9a15-4fa8-b5d5-26811102258f',
    CURRENT_DATE - 7,
    CURRENT_DATE + 11,
    'IHGHXYM',
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
    '04206c50-9a15-4fa8-b5d5-26811102258f',
    CURRENT_DATE,
    CURRENT_DATE + 11,
    'cc3add97-2b68-4cb6-9142-6cb6842223ee',
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
    'bfdf867a-4f0e-4462-b388-b05af315c140',
    CURRENT_DATE - 7,
    CURRENT_DATE + 20,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 20,
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
    'bfdf867a-4f0e-4462-b388-b05af315c140',
    CURRENT_DATE,
    CURRENT_DATE + 20,
    '1575ea9d-7824-4a24-a3bb-dba83a978094',
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
    '9f904a9b-ca17-4648-a9d8-4c934a6689d9',
    CURRENT_DATE - 7,
    CURRENT_DATE + 39,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 39,
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
    '9f904a9b-ca17-4648-a9d8-4c934a6689d9',
    CURRENT_DATE,
    CURRENT_DATE + 39,
    'ffe2c15a-90de-44ab-b8fb-4843e2ac109f',
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
    '4e77927b-7ccf-4e80-8b38-335b3e4f501c',
    CURRENT_DATE - 7,
    CURRENT_DATE + 35,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 35,
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
    '4e77927b-7ccf-4e80-8b38-335b3e4f501c',
    CURRENT_DATE,
    CURRENT_DATE + 35,
    '0d59544b-b839-4619-b698-662eba439cfc',
    NULL
  );
  
  
