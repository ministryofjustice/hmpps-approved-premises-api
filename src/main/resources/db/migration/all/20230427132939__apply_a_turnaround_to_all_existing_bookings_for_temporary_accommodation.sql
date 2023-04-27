INSERT INTO turnarounds (id, booking_id, working_day_count, created_at)
SELECT
    gen_random_uuid(),
    b.id,
    0,
    now()
FROM bookings b
WHERE b.service = 'temporary-accommodation';
