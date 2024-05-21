CREATE TABLE public.domain_events_metadata (
    domain_event_id uuid NOT NULL,
    "name" text NOT NULL,
    value text NULL,
    CONSTRAINT domain_events_metadata_unique UNIQUE (domain_event_id,"name"),
    CONSTRAINT domain_events_metadata_domain_events_fk FOREIGN KEY (domain_event_id) REFERENCES public.domain_events(id)
);
CREATE INDEX domain_events_metadata_domain_event_id_idx ON public.domain_events_metadata (domain_event_id,"name");