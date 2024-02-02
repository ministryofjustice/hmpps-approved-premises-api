-- create new table for CAS2 status updates
CREATE TABLE cas_2_status_update_details (
    id               UUID                        NOT NULL,
    status_update_id UUID                        NOT NULL,
    status_detail_id UUID                        NOT NULL,
    label            TEXT                        NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_status_update_id
        FOREIGN KEY(status_update_id)
    	    REFERENCES cas_2_status_updates(id)

);
-- index for status updates to cas_2_status_updates table
CREATE INDEX ON cas_2_status_update_details(status_update_id);

