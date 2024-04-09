CREATE TABLE cas_3_duty_to_refer_outcome (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name)
);

INSERT INTO cas_3_duty_to_refer_outcome(id, name, is_active)
VALUES
      ('7346f068-c7a7-4fff-848a-1b14aa811f23', 'Accepted – Prevention/ Relief Duty', true),
      ('208c9a23-05a4-44da-8ea4-8a93c5598620', 'Accepted – Priority Need', true),
      ('bc53da36-45f9-4752-9062-fc738dbcad98', 'Rejected – No Local Connection', true),
      ('25f2b233-9323-4113-9129-80ba5b5db27a', 'Rejected – Other', true),
      ('82817fab-a72e-4668-a684-b118b9da1d94', 'Rejected – Intentionally Homeless', true),
      ('11cf63ce-8b68-4297-89c5-385aadb3d80e', 'Pending', true);
