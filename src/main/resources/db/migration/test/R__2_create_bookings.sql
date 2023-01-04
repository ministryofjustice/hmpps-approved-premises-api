
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
    "service",
    "created_at"
  )
VALUES
  (
    '073ceefb-6bfb-4425-9b36-96bcb0330af3',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '315VWWC',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    "service",
    "created_at"
  )
VALUES
  (
    'cf38f87c-4d64-43d5-ad31-d0f0c03e65f7',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4Y29R9P',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    "service",
    "created_at"
  )
VALUES
  (
    '58b8b2e1-0948-4374-82e4-27b1c848808e',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '4ZUIHFX',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
    'approved-premises',
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
    "service",
    "created_at"
  )
VALUES
  (
    '38be7810-55a5-448a-9b74-67d0ea2c6bea',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    '52W7TQG',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    "service",
    "created_at"
  )
VALUES
  (
    '9ae6fefb-873b-44b8-bab0-ae4f58fef451',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '5EC66UT',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    "service",
    "created_at"
  )
VALUES
  (
    '58888937-c0a6-4e4e-9d59-103ade3cdab3',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    'BWEFOI7',
    CURRENT_DATE + 4,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    "service",
    "created_at"
  )
VALUES
  (
    'd5469aca-ada9-469e-b364-7a13972d537f',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    'GSR1T2F',
    CURRENT_DATE + 2,
    CURRENT_DATE + 84,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    "service",
    "created_at"
  )
VALUES
  (
    '08c1f449-114f-43bc-beb3-8f18fdb8b82e',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    '08c1f449-114f-43bc-beb3-8f18fdb8b82e',
    CURRENT_DATE,
    CURRENT_DATE,
    '31d49adc-e7b6-462d-9203-0f6d9a5d107e',
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
    "service",
    "created_at"
  )
VALUES
  (
    '5471d52d-e622-45c1-ad4a-5c743a2238df',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    '5471d52d-e622-45c1-ad4a-5c743a2238df',
    CURRENT_DATE,
    CURRENT_DATE,
    '4f3bdce0-0403-4fbd-a033-76355e090b9b',
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
    "service",
    "created_at"
  )
VALUES
  (
    '5bc77984-a1bf-4441-831a-d0b02db8ede7',
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE + 3,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    '5bc77984-a1bf-4441-831a-d0b02db8ede7',
    CURRENT_DATE,
    CURRENT_DATE + 3,
    '640255ea-b8ec-4680-a942-8725cc713393',
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
    "service",
    "created_at"
  )
VALUES
  (
    '6124de81-31f6-40ef-8059-86f433f385e2',
    CURRENT_DATE - 84,
    CURRENT_DATE + 2,
    'HTVI42B',
    CURRENT_DATE - 84,
    CURRENT_DATE + 2,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    '6124de81-31f6-40ef-8059-86f433f385e2',
    CURRENT_DATE,
    CURRENT_DATE + 2,
    '0cd204c3-ea79-4595-9b8f-9f27a0bf57dd',
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
    "service",
    "created_at"
  )
VALUES
  (
    'ad46c28a-883b-47ea-a746-eebd1feda4fa',
    CURRENT_DATE - 7,
    CURRENT_DATE + 7,
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 7,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    'ad46c28a-883b-47ea-a746-eebd1feda4fa',
    CURRENT_DATE,
    CURRENT_DATE + 7,
    '57d00f36-f13c-4d90-bcb9-c7fe1218d810',
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
    "service",
    "created_at"
  )
VALUES
  (
    '1c177eaf-30cf-4c1a-9f3f-21ec009328e7',
    CURRENT_DATE - 7,
    CURRENT_DATE + 31,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 31,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    '1c177eaf-30cf-4c1a-9f3f-21ec009328e7',
    CURRENT_DATE,
    CURRENT_DATE + 31,
    '4041b12c-0ec0-40a3-b678-660ad7705180',
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
    "service",
    "created_at"
  )
VALUES
  (
    '18744e00-94f3-460f-9f83-87397549442f',
    CURRENT_DATE - 7,
    CURRENT_DATE + 59,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 59,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    '18744e00-94f3-460f-9f83-87397549442f',
    CURRENT_DATE,
    CURRENT_DATE + 59,
    'c2a45301-3486-44a1-bb14-39f643468838',
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
    "service",
    "created_at"
  )
VALUES
  (
    '4a155f3f-5672-4d71-ae42-e9e32b6cf12c',
    CURRENT_DATE - 7,
    CURRENT_DATE + 15,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 15,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    '4a155f3f-5672-4d71-ae42-e9e32b6cf12c',
    CURRENT_DATE,
    CURRENT_DATE + 15,
    'c3f3adb2-fe21-4703-b6ba-8cabfb31a66e',
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
    "service",
    "created_at"
  )
VALUES
  (
    '21bab63c-03f1-48ff-85b1-cad092aca9ee',
    CURRENT_DATE - 7,
    CURRENT_DATE + 31,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 31,
    '459eeaba-55ac-4a1f-bae2-bad810d4016b',
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
    '21bab63c-03f1-48ff-85b1-cad092aca9ee',
    CURRENT_DATE,
    CURRENT_DATE + 31,
    'b145fc80-9fab-46df-b224-941f4526fa18',
    NULL
  );
  
  
