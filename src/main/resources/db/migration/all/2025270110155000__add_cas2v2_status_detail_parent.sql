

ALTER TABLE cas_2_v2_status_update_details ADD COLUMN parent_id UUID;

ALTER TABLE cas_2_v2_status_update_details
    ADD CONSTRAINT FK_CAS_2_V2_STATUS_UPDATE_DETAILS_PARENT FOREIGN KEY (parent_id) REFERENCES cas_2_v2_status_update_details (id);
