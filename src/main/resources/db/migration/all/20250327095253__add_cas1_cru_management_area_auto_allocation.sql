CREATE TABLE cas1_cru_management_area_auto_allocations (
    cas1_cru_management_area_id uuid NOT NULL,
    "day" text NOT NULL,
    delius_username text NOT NULL,
    CONSTRAINT cas1_cru_management_area_auto_allocations_pk PRIMARY KEY (cas1_cru_management_area_id, "day"),
    CONSTRAINT cas1_cru_management_area_auto_allocations_unique UNIQUE (cas1_cru_management_area_id,"day"),
    CONSTRAINT cas1_cru_management_area_auto_allocations_check CHECK (day = 'MONDAY' OR day = 'TUESDAY' OR day = 'WEDNESDAY' OR day = 'THURSDAY' OR day = 'FRIDAY' OR day = 'SATURDAY' OR day = 'SUNDAY'),
	CONSTRAINT cas1_cru_management_area_auto_allocations_cas1_cru_management_areas_fk FOREIGN KEY (cas1_cru_management_area_id) REFERENCES cas1_cru_management_areas(id)
);
