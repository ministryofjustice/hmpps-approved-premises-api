-- Delete all rooms belonging to temporary-accommodation CAS3 premises from the rooms table
DELETE FROM rooms
WHERE premises_id IN (
  SELECT id FROM premises WHERE service = 'temporary-accommodation'
);
