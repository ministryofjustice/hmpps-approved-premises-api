
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
    '02c669de-5cd4-4599-906f-9dd846a6977e',
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
    '47f7d7b7-09dd-4214-90a7-74b16c276566',
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
    '170ed76c-97aa-46eb-a8c6-56465203ce03',
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
    'f453cd2f-258b-406b-9d59-3f91efc26a94',
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
    '6035000f-c2e6-4474-b5cc-640cf9a60660',
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
    '7e64450c-c0ef-400d-a4f3-156d663f60ef',
    CURRENT_DATE + 1,
    CURRENT_DATE + 84,
    'BWEFOI7',
    CURRENT_DATE + 1,
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
    'e091ccf8-b61e-4587-9899-cc7673f38f1c',
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
    'c6c17927-589b-4eb6-aca8-64fcd52fc620',
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
    'c6c17927-589b-4eb6-aca8-64fcd52fc620',
    CURRENT_DATE,
    CURRENT_DATE,
    '666aa21e-0e5b-460b-9f12-cba787bb2e21',
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
    '4aabf4ac-43c9-40fa-9784-67463f489bb4',
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
    '4aabf4ac-43c9-40fa-9784-67463f489bb4',
    CURRENT_DATE,
    CURRENT_DATE,
    'fcc5c100-a3c6-4ffb-8f62-838e76e19799',
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
    '7b1fd735-5634-464a-8fe2-3cda0ec029b5',
    CURRENT_DATE - 84,
    CURRENT_DATE + 1,
    'HRV83TE',
    CURRENT_DATE - 84,
    CURRENT_DATE + 1,
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
    '7b1fd735-5634-464a-8fe2-3cda0ec029b5',
    CURRENT_DATE,
    CURRENT_DATE + 1,
    'd2b5426b-2647-4d3d-9c51-448bae3c3e82',
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
    '0a6cb591-84f1-468b-8a96-23fde59b8e8b',
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
    '0a6cb591-84f1-468b-8a96-23fde59b8e8b',
    CURRENT_DATE,
    CURRENT_DATE + 2,
    '82dca5e9-452b-4d77-a568-7691bd869190',
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
    '2c684d44-0b32-4538-a23a-03b88d3e31c8',
    CURRENT_DATE - 7,
    CURRENT_DATE + 44,
    'HUN3BN0',
    CURRENT_DATE - 7,
    CURRENT_DATE + 44,
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
    '2c684d44-0b32-4538-a23a-03b88d3e31c8',
    CURRENT_DATE,
    CURRENT_DATE + 44,
    'c9c8ac4c-274a-4dae-82e1-6a2f8fcbf41b',
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
    'e9e076c3-4470-4bc6-ad63-579edbc67348',
    CURRENT_DATE - 7,
    CURRENT_DATE + 60,
    'IHGHXYM',
    CURRENT_DATE - 7,
    CURRENT_DATE + 60,
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
    'e9e076c3-4470-4bc6-ad63-579edbc67348',
    CURRENT_DATE,
    CURRENT_DATE + 60,
    'eb26a525-dca3-42b4-bfd7-dc2113b6b861',
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
    '7ea914c2-d404-4ecc-96ef-30f9fbb964be',
    CURRENT_DATE - 7,
    CURRENT_DATE + 54,
    'JCRH9V5',
    CURRENT_DATE - 7,
    CURRENT_DATE + 54,
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
    '7ea914c2-d404-4ecc-96ef-30f9fbb964be',
    CURRENT_DATE,
    CURRENT_DATE + 54,
    'fb10aa74-f7ab-42b3-be7c-e3830147c98e',
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
    '1802fb60-252a-4cce-974b-81d37a55ae28',
    CURRENT_DATE - 7,
    CURRENT_DATE + 56,
    'N6OUTAY',
    CURRENT_DATE - 7,
    CURRENT_DATE + 56,
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
    '1802fb60-252a-4cce-974b-81d37a55ae28',
    CURRENT_DATE,
    CURRENT_DATE + 56,
    '0a669c71-5dde-49d2-9752-a8e01739d43b',
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
    '81cd1404-549e-4aff-9e2e-212bc71b79d3',
    CURRENT_DATE - 7,
    CURRENT_DATE + 21,
    'PR5E5Y2',
    CURRENT_DATE - 7,
    CURRENT_DATE + 21,
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
    '81cd1404-549e-4aff-9e2e-212bc71b79d3',
    CURRENT_DATE,
    CURRENT_DATE + 21,
    'd2d5ebdb-e877-4c0e-8a84-ac4bcf21dd26',
    NULL
  );
  
  
