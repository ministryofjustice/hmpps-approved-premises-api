CREATE TABLE prison_release_types (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    is_active BOOL NOT NULL,
    service_scope TEXT NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (name)
);

INSERT INTO prison_release_types(id,name,is_active,service_scope,sort_order) VALUES
    ('82ce603b-a7d1-4e65-bab5-ed750917840a','Conditional release date (CRD) licence',true,'temporary-accommodation',1),
    ('f750a14e-a815-49d8-a70d-901734c16f27','End of custody supervised licence (ECSL)',true,'temporary-accommodation',2),
    ('9ffa8a3a-bd8a-472b-9302-3e97c433eec7','Licence, following fixed-term recall',true,'temporary-accommodation',3),
    ('9b887b63-02f0-4441-b609-abf5f44fd261','Licence, following standard recall',true,'temporary-accommodation',4),
    ('d8fa5a15-bddf-4f51-ac0f-4c8f7ed5dfd8','Parole',true,'temporary-accommodation',5),
    ('740f6dd5-1ec2-493b-893e-ca2406464e36','Post sentence supervision (PSS)',true,'temporary-accommodation',6);
