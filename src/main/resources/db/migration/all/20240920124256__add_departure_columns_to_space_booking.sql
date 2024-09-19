ALTER TABLE cas1_space_bookings ADD departure_reason_id UUID NULL;
ALTER TABLE cas1_space_bookings ADD departure_move_on_category_id UUID NULL;

ALTER TABLE cas1_space_bookings ADD FOREIGN KEY (departure_reason_id) REFERENCES departure_reasons(id);
ALTER TABLE cas1_space_bookings ADD FOREIGN KEY (departure_move_on_category_id) REFERENCES move_on_categories(id);
