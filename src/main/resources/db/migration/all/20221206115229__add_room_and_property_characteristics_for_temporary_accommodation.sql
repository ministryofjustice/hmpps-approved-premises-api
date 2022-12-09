INSERT INTO characteristics (id, name, service_scope, model_scope)
VALUES
    -- Property characteristics
    ('fffb3004-5f0a-4e88-8350-fb89a0168296', 'Park nearby', 'temporary-accommodation', 'premises'),
    ('684f919a-4c4a-4e80-9b3a-1dcd35873b3f', 'Pub nearby', 'temporary-accommodation', 'premises'),
    ('78c5d99b-9702-4bf2-a23d-c2a0cf3a017d', 'School nearby', 'temporary-accommodation', 'premises'),
    ('2fff6ede-7035-4e8d-81ad-5fcd894b99cf', 'Men only', 'temporary-accommodation', 'premises'),
    ('8221f1ad-3aaf-406a-b918-dbdef956ea17', 'Women only', 'temporary-accommodation', 'premises'),
    -- Room characteristics
    ('12e2e689-b3fb-469d-baec-2fb68e15e85b', 'Single bed', 'temporary-accommodation', 'room'),
    ('08b756e2-0b82-4f49-a124-35ea4ebb1634', 'Double bed', 'temporary-accommodation', 'room'),
    ('7dd3bac5-3d1c-4acb-b110-b1614b2c95d8', 'Shared kitchen', 'temporary-accommodation', 'room'),
    ('e730bdea-6157-4910-b1b0-450b29bf0c9f', 'Shared bathroom', 'temporary-accommodation', 'room'),
    ('2183d873-8270-4e93-8518-3a668a053689', 'Lift access', 'temporary-accommodation', 'room'),
    -- Shared characteristics
    ('a2dd4661-c7fe-4294-b3fd-3fc877e62aa5', 'Not suitable for registered sex offenders (RSO)', 'temporary-accommodation', '*'),
    ('62c4d8cf-b612-4110-9e27-5c29982f9fcf', 'Not suitable for arson offenders', 'temporary-accommodation', '*'),
    ('99bb0f33-ff92-4606-9d1c-43bcf0c42ef4', 'Floor level access', 'temporary-accommodation', '*'),
    ('d2f7796a-88e5-4e53-ab6d-dabb145b6a60', 'Wheelchair accessible', 'temporary-accommodation', '*');
