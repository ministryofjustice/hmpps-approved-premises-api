CREATE TABLE cas1_cru_management_areas (
    id uuid NOT NULL,
    "name" text NOT NULL,
    email_address text NULL,
    notify_reply_to_email_id text NOT NULL,
    CONSTRAINT cas1_cur_management_area_pkey PRIMARY KEY (id)
);