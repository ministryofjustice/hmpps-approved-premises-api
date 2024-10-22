INSERT INTO premises (id, "name", postcode, probation_region_id, local_authority_area_id, service, notes, address_line1) VALUES
  ('b4208523-7594-4f72-9be0-488141388309', 'Womens AP 1', 'GL56 0QQ', '0544d95a-f6bb-43f8-9be7-aae66e3bf244', '7de4177b-9177-4c28-9bb6-5f5292619546', 'approved-premises', 'Notes', '3 Somewhere'),
  ('78edba43-783d-49e9-9e3a-c82962566a7f', 'Womens AP 2', 'GL56 0QQ', '0544d95a-f6bb-43f8-9be7-aae66e3bf244', '7de4177b-9177-4c28-9bb6-5f5292619546', 'approved-premises', 'Notes', '4 Somewhere');

INSERT INTO approved_premises (premises_id, q_code, ap_code, point, gender) VALUES
  ('b4208523-7594-4f72-9be0-488141388309', 'Q007', 'WMN', null, 'WOMAN'),
  ('78edba43-783d-49e9-9e3a-c82962566a7f', 'Q008', 'WMN2', null, 'WOMAN');
