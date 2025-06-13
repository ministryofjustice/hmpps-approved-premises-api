DELETE
FROM characteristics;

INSERT INTO characteristics (id, name, service_scope)
VALUES ('952790c0-21d7-4fd6-a7e1-9018f08d8bb0', 'Park nearby', 'temporary-accommodation'),
       ('199334d3-fabb-432f-84c3-0e92eaf13f24', 'Pub nearby', 'approved-premises'),
       ('1dac11c5-a1b4-4313-b0f7-41b2e57f2404', 'School nearby', 'temporary-accommodation'),
       ('94021062-f692-4877-b6e8-f36c7ff87a18', 'Not suitable for registered sex offenders (RSO)', 'approved-premises'),
       ('16b52729-be75-43b4-b711-77fe5cbcb477', 'Not suitable for arson offenders', 'temporary-accommodation'),
       ('7846fbf2-b423-4ccc-b23b-d8b866f86bde', 'Men only', 'approved-premises'),
       ('7cd8dd2c-a14d-4a78-8d72-48449cc5aaa3', 'Women only', 'temporary-accommodation'),
       ('d9f52ae8-4c01-4751-8975-992efacd5260', 'Floor level access', 'temporary-accommodation'),
       ('a862be08-a96a-4337-9f46-26286db8015f', 'Wheelchair accessible', 'approved-premises');