ALTER TABLE cas1_space_bookings ADD key_worker_user_id uuid NULL;
ALTER TABLE cas1_space_bookings ADD CONSTRAINT cas1_space_bookings_users_fk FOREIGN KEY (key_worker_user_id) REFERENCES public.users(id);
