ALTER TABLE domain_events ADD cas1_placement_request_id UUID NULL;
ALTER TABLE domain_events ADD FOREIGN KEY (cas1_placement_request_id) REFERENCES placement_requests(id);
CREATE INDEX domain_events_cas1_placement_request_id_idx ON domain_events (cas1_placement_request_id);