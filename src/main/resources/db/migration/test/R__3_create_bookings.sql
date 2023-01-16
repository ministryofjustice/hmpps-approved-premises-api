
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
    'e8b04ceb-95a4-41be-b147-f34bd11393a9',
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
    '84bb08e8-2c36-461a-8f32-1ab55af07d8a',
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
    '731e55f3-628d-4492-8927-0174e496c092',
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
    'e88604c0-ed8d-4a8b-b7c0-e9954559ab82',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'PR5E5Y2',
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
    '30484627-f3c5-4866-aee9-13488f1b2ac5',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'N6OUTAY',
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
    '8f65b158-2371-40d4-a2e2-c3d60b5d5558',
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
    'd8bdae03-7f6a-4674-be0d-9ea081526b62',
    CURRENT_DATE,
    CURRENT_DATE + 84,
    'IHGHXYM',
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
    '8a374cd1-f985-4a14-81e2-d2bd4423b037',
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
    '5d3a2a8e-85d8-48ca-8722-8e0c00a45210',
    CURRENT_DATE + 3,
    CURRENT_DATE + 84,
    '52W7TQG',
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
    'b884d51b-64cb-46ed-b238-3d05f64bf32f',
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
    '8fe80127-0af7-4b4c-875f-4949642837c9',
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
    '594a5a6f-3e8b-4041-9ac2-d0940e1c88e2',
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
    '9a928d61-a81e-4b05-81d9-bdf0cdfae87e',
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
    '9a928d61-a81e-4b05-81d9-bdf0cdfae87e',
    CURRENT_DATE,
    CURRENT_DATE,
    'fb031350-cbc1-4806-b655-961423834a51',
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
    '84429449-620d-4c54-a96b-1737405d5d19',
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
    '84429449-620d-4c54-a96b-1737405d5d19',
    CURRENT_DATE,
    CURRENT_DATE,
    '05128d6f-0857-4878-a068-65b4792c3055',
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
    '67ecea03-8714-4b4e-84e3-53ceda7ff950',
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
    '67ecea03-8714-4b4e-84e3-53ceda7ff950',
    CURRENT_DATE,
    CURRENT_DATE + 2,
    'c496d038-0646-48a2-bfc6-1abe7c7fa3e0',
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
    '7a3849e1-10ed-4b7c-bf30-e4c53b63c9d8',
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
    '7a3849e1-10ed-4b7c-bf30-e4c53b63c9d8',
    CURRENT_DATE,
    CURRENT_DATE + 4,
    'e65f0e9a-0d46-4547-86ec-0124853d8ba4',
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
    'd3a4dc33-5915-4e75-a647-ba15801a6071',
    CURRENT_DATE - 7,
    CURRENT_DATE + 46,
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 46,
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
    'd3a4dc33-5915-4e75-a647-ba15801a6071',
    CURRENT_DATE,
    CURRENT_DATE + 46,
    '75448ffc-10a1-4639-aa11-7882e3e81c7e',
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
    'e8723a18-3da2-4b24-9c98-da56f36b07c5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 15,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 15,
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
    'e8723a18-3da2-4b24-9c98-da56f36b07c5',
    CURRENT_DATE,
    CURRENT_DATE + 15,
    'f19733aa-dac5-4b52-8781-1434f7b6ec1c',
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
    '3d35aa22-25c0-40b7-9174-e64dd7aa9e27',
    CURRENT_DATE - 7,
    CURRENT_DATE + 17,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 17,
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
    '3d35aa22-25c0-40b7-9174-e64dd7aa9e27',
    CURRENT_DATE,
    CURRENT_DATE + 17,
    'e43a91aa-2f94-462b-bfb8-f085a8f69f51',
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
    '3562ae4e-c1ff-413e-885c-483e7f91a046',
    CURRENT_DATE - 7,
    CURRENT_DATE + 21,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 21,
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
    '3562ae4e-c1ff-413e-885c-483e7f91a046',
    CURRENT_DATE,
    CURRENT_DATE + 21,
    '9aba40a4-508e-4074-b191-ddddf46c0a49',
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
    '525c4773-b6a8-41f2-b543-2a613be69a61',
    CURRENT_DATE - 7,
    CURRENT_DATE + 43,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 43,
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
    '525c4773-b6a8-41f2-b543-2a613be69a61',
    CURRENT_DATE,
    CURRENT_DATE + 43,
    '5a0eb398-95da-4da0-be0e-862b295fe82b',
    NULL
  );
  
  
