ALTER TABLE domain_events_metadata ADD id uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE domain_events_metadata ADD CONSTRAINT domain_events_metadata_pk PRIMARY KEY (id);

ALTER TABLE domain_events_metadata DROP CONSTRAINT domain_events_metadata_unique;
