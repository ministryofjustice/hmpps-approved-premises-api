ALTER TABLE temporary_accommodation_applications
    ADD is_history_of_arson_offence BOOLEAN,
    ADD is_concerning_arson_behaviour BOOLEAN,
    ADD concerning_arson_behaviour TEXT NULL;