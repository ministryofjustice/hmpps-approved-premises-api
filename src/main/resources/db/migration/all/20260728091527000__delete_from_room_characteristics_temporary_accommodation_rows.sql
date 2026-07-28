-- Delete from room_characteristics table all characteristics belonging to temporary-accommodation CAS3 premises
DELETE FROM room_characteristics
WHERE room_id IN (
    SELECT r.id
    FROM rooms r
             JOIN premises p ON p.id = r.premises_id
    WHERE p.service = 'temporary-accommodation'
);