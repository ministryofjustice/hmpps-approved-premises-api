CREATE TABLE departure_reason (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    is_active BOOL NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE TABLE move_on_category (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    is_active BOOL NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE TABLE destination_provider (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    is_active BOOL NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE TABLE booking (
    id UUID NOT NULL,
    arrival_date DATE NOT NULL,
    departure_date DATE NOT NULL,
    key_worker TEXT NOT NULL,
    premises_id UUID NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE person (
    id UUID NOT NULL,
    crn TEXT NOT NULL,
    name TEXT NOT NULL,
    booking_id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE TABLE arrival (
    id UUID NOT NULL,
    arrival_date DATE NOT NULL,
    expected_departure_date DATE NOT NULL,
    notes TEXT,
    booking_id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE TABLE departure (
    id UUID NOT NULL,
    date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    departure_reason_id UUID NOT NULL,
    move_on_category_id UUID NOT NULL,
    destination_provider_id UUID NOT NULL,
    notes TEXT,
    booking_id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (departure_reason_id) REFERENCES departure_reason(id),
    FOREIGN KEY (destination_provider_id) REFERENCES destination_provider(id),
    FOREIGN KEY (move_on_category_id) REFERENCES move_on_category(id)
);

CREATE TABLE non_arrival (
    id UUID NOT NULL,
    "date" DATE NOT NULL,
    reason TEXT NOT NULL,
    notes TEXT,
    booking_id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

CREATE TABLE cancellation (
    id UUID NOT NULL,
    "date" DATE NOT NULL,
    reason TEXT NOT NULL,
    notes TEXT,
    booking_id UUID NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES booking(id)
);

INSERT INTO departure_reason (id, name, is_active) VALUES
    ('ea6f21d9-e658-487f-9765-d8b09187df93', 'Absconded, still at large', true),
    ('5bb2a91b-a33a-425e-8503-2ba69d7cabe7', 'Admitted to Hospital', true),
    ('861b3403-7dd9-440f-86e9-0c164ca0812b', 'Arrested, remanded in custody, or sentenced', true),
    ('9a68687d-944b-4960-9de2-7645635910f3', 'Bed Withdrawn', true),
    ('c81c42fe-f7af-4d16-b67d-93f0b4cbd338', 'Breach / recall (abscond)', true),
    ('f5b57d36-1b3c-4777-93a2-72ff59cf7674', 'Breach / recall (behaviour / increasing risk)', true),
    ('b80cbd68-c223-40af-b82b-5f7cee6f6fd7', 'Breach / recall (curfew)', true),
    ('ff751e28-f957-4941-8049-5b44c6da878e', 'Breach / recall (house rules)', true),
    ('d6e3f1db-01a7-4ce4-b7cb-f1ff5454d62e', 'Breach / recall (licence or bail condition)', true),
    ('d86b6e2e-0b95-459a-a85a-065729149f0a', 'Breach / recall (other)', true),
    ('761cfbe1-c102-4fe9-b943-bf131d550c90', 'Breach / recall (positive drugs test)', true),
    ('69eac20f-c3b9-4ea3-87fd-814c3f1fd117', 'Died', true),
    ('5b7662e3-f0d9-401d-baeb-d8ad391dc8bb', 'End of ROTL', true),
    ('6589f8fb-d377-4bc4-ace9-d3a2ce02f97b', 'Left of own volition', true),
    ('9c848d97-afe7-4da9-bd8b-f01897330e62', 'Order / licence expired', true),
    ('60d030db-ce2d-4f1a-b24f-42ce15d268f7', 'Other', true),
    ('f4d00e1c-8bfd-40e9-8241-a7d0f744e737', 'Planned move-on', true);

INSERT INTO move_on_category (id, name, is_active) VALUES
    ('44bb3d42-c162-455a-b16b-d998961f98fd', 'B & B / Temp / Short-Term Housing', true),
    ('48ad4a94-f81f-4cd5-a564-ad1974d5cf67', 'Custody', true),
    ('e0dacde6-ddcf-4c20-af58-8fd858eb4171', 'Deported/ Detention Centre/IRC', true),
    ('55d9f90f-454a-410e-b02d-4dc8ba536ac2', 'Housing Association - Rented', true),
    ('e11e59e4-75fe-462c-9850-80764b95acc7', 'Living with Family/ Partner/ Other', true),
    ('5387c2ed-b57f-4c31-8fd9-fee30ef32197', 'Local Authority - Rented', true),
    ('e1436d4b-5723-4e92-b53c-2b6c6babe0b3', 'No Fixed Abode', true),
    ('ea3d79b0-1ee5-47ff-a7ae-b0d964ca7626', 'Not Applicable', true),
    ('ff40dd76-c9eb-48a5-9ac8-9f51f03dbf48', 'Owner Occupied', true),
    ('9ed75736-83bb-4387-9eec-19a6e6343357', 'Private Rented', true),
    ('0d8dee49-f49a-4e0e-a20e-80e0c18645ce', 'Supported Housing', true),
    ('6b1f6645-dc1c-489d-8312-cab9a4a6b2a7', 'Transferred to different AP', true);

INSERT INTO destination_provider (id, name, is_active) VALUES
    ('573f7e61-e15e-4502-b331-1c7058957021', 'Central Projects Team', true),
    ('6cd6fd2d-f298-4e2e-a3ef-b4273a859e41', 'Commissioned Rehab Services', true),
    ('2bbc6591-435b-4b3a-9431-5a5cfe904922', 'East Midlands Region', true),
    ('1824c41d-7966-4bb5-82d8-f5f8332d0342', 'East of England', true),
    ('25d81028-7ada-4af8-8561-ad1dea390dc9', 'Ext - East Midlands Region', true),
    ('c6e3e602-8d63-431e-bb3e-cb866c2bbfb0', 'Ext - East of England', true),
    ('57ad6a08-7e0e-475c-986a-be027dcb3114', 'Ext - Greater Manchester', true),
    ('0536ba78-d3ab-4249-aa21-1ee5a5af72e0', 'Ext - Kent Surrey Sussex', true),
    ('5c2b0c23-9702-4fbe-b013-6f2c4fa0e194', 'Ext - North East Region', true),
    ('887cf6d5-5152-4749-b113-3715d967af39', 'Ext - North West Region', true),
    ('ab4db19c-7110-45d3-8aab-2a2f16dc6d80', 'Ext - South Central', true),
    ('46c92ee1-dd23-4dd3-a355-0da9026b858a', 'Ext - South West', true),
    ('f4f1fc60-3040-4e3c-b4ef-27f31b3234f3', 'Ext - West Midlands Region', true),
    ('1260a558-0545-4c23-a199-b7da514fa67c', 'Ext - Yorkshire and The Humber', true),
    ('b24507ff-14e6-4bff-8779-c9d383d58ac4', 'External - London', true),
    ('305964d5-16d3-4c42-8772-3835ca91ab1e', 'External - Wales', true),
    ('5b113fcd-02aa-4809-806e-449d2724d2f0', 'Greater Manchester', true),
    ('b688734c-bfd2-4c70-aa7f-6b81ce42da5c', 'Kent Surrey Sussex Region', true),
    ('607ad0ce-b74e-4d66-9a20-9d2956afff6d', 'London', true),
    ('25e2907d-a676-4432-a48b-4864f5d4b1bd', 'National Responsibility Division', true),
    ('3f359c09-5844-4e5c-a086-4a57f91bf995', 'National Security Division', true),
    ('15e0dc30-44f8-45fe-91a1-ebdb36aa7ad9', 'No Trust or Trust Unknown', true),
    ('18abaafc-56f8-4eaa-bdee-370bcf3324a9', 'North East Region', true),
    ('49ae117f-c585-466c-905f-34aced8701d9', 'North West Region', true),
    ('35ce5b27-f2bd-4315-a363-c84dd9c713fc', 'South Central', true),
    ('2068c488-4069-454f-8596-539a7a2de378', 'South West', true),
    ('4ab0fdf0-8d93-437f-9695-3f397a683464', 'Wales', true),
    ('988c6422-00fe-4c83-9091-9ac92d93f14d', 'West Midlands Region', true),
    ('82cebdcf-b3eb-4d65-b4be-0903d7e7ba52', 'Yorkshire and The Humber', true),
    ('66240361-9355-4a58-a806-0f3e32419448', 'ZZ BAST Public Provider 1', true);
