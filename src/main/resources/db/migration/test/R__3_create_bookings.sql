
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
    '2dd0ef31-d264-4dee-976d-0be16602e1bb',
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
    '21065f54-6fd0-4382-b2ac-0f9a829bbd27',
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
    'd103fa39-2008-443e-99d4-000b813a7bd3',
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
    '63134b97-c2f0-45a7-8379-29499deb0eb7',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'JCRH9V5',
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
    '731f6703-58df-4138-9c9d-7f4a8a836601',
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
    '1c9b569c-3818-4756-bba0-cb83c03d9bb1',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'N6OUTAY',
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
    'b65e8e38-9c9e-4dd5-b3bc-9224f421985b',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'QA93YYK',
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
    '6fb510a7-f51b-4654-94d7-e2764413166b',
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
    '17c59243-eec9-4f81-9f72-68be44952f2c',
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
    'a5fa0bde-e480-4035-9a20-28cfa21cecdb',
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
    '911b5e1f-aa96-4aa2-875f-3fec17c2509d',
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
    '3b91bbdf-f389-4504-b71e-3c4c05016a05',
    CURRENT_DATE + 3,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE + 3,
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
    '7a2bedde-623a-4f77-a37b-2a98c8bb0026',
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
    '7a2bedde-623a-4f77-a37b-2a98c8bb0026',
    CURRENT_DATE,
    CURRENT_DATE,
    'f72650ff-0c59-4289-950e-a23cd9dbef01',
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
    '25cc579f-3d1f-485a-90e4-68b2d9198642',
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
    '25cc579f-3d1f-485a-90e4-68b2d9198642',
    CURRENT_DATE,
    CURRENT_DATE,
    '6e3bceb5-f8f7-4a57-81a9-2191f7175248',
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
    '1352c9fb-26ef-493d-bbe2-bb3b08b15907',
    CURRENT_DATE - 84,
    CURRENT_DATE + 2,
    'HRV83TE',
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
    '1352c9fb-26ef-493d-bbe2-bb3b08b15907',
    CURRENT_DATE,
    CURRENT_DATE + 2,
    '5818bbcc-e4f8-472e-85af-1280ae4f0858',
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
    'b965e77b-5721-4209-98dc-4914f6ac1d88',
    CURRENT_DATE - 84,
    CURRENT_DATE + 1,
    'HTVI42B',
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
    'b965e77b-5721-4209-98dc-4914f6ac1d88',
    CURRENT_DATE,
    CURRENT_DATE + 1,
    '2f3216f6-0435-4d51-997d-cd9a4cc1f109',
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
    'bdbf1efb-f469-4888-ad51-4ceba781be89',
    CURRENT_DATE - 7,
    CURRENT_DATE + 8,
    'HUN3BN0',
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
    'bdbf1efb-f469-4888-ad51-4ceba781be89',
    CURRENT_DATE,
    CURRENT_DATE + 8,
    'c442e7a3-6c0d-40a3-b1a7-ad0e4d6a2832',
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
    '77565fdf-72f0-4c08-9f34-332248199819',
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
    '77565fdf-72f0-4c08-9f34-332248199819',
    CURRENT_DATE,
    CURRENT_DATE + 11,
    '25d08203-7018-4183-9bab-0d066cc6781c',
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
    '15640622-83eb-4c94-8e73-1ee41883dad4',
    CURRENT_DATE - 7,
    CURRENT_DATE + 1,
    'JCRH9V5',
    CURRENT_DATE - 7,
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
    CURRENT_DATE - 7,
    '15640622-83eb-4c94-8e73-1ee41883dad4',
    CURRENT_DATE,
    CURRENT_DATE + 1,
    'd5fdaa77-3a80-4234-a5a5-7bd9a61a7079',
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
    '87f7421f-1fc6-4ee1-8e49-2a3e4dc71738',
    CURRENT_DATE - 7,
    CURRENT_DATE + 38,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 38,
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
    '87f7421f-1fc6-4ee1-8e49-2a3e4dc71738',
    CURRENT_DATE,
    CURRENT_DATE + 38,
    '8f042e87-03c3-46eb-a710-f1674c2f7d8c',
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
    'f6a99918-a1a5-4f10-bbb1-ded610a45636',
    CURRENT_DATE - 7,
    CURRENT_DATE + 1,
    'PR5E5Y2',
    CURRENT_DATE - 7,
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
    CURRENT_DATE - 7,
    'f6a99918-a1a5-4f10-bbb1-ded610a45636',
    CURRENT_DATE,
    CURRENT_DATE + 1,
    'ec095ec2-8ca7-4a58-8eef-452597ed4fb7',
    NULL
  );
  
  
