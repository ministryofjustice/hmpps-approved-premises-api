BEGIN TRANSACTION;

DELETE FROM cas_2_status_updates
WHERE application_id IN (
  'a0cca0dc-2fad-4286-b380-4585e8a88bc0',
  '323b4ae1-d63e-4af8-9f5e-010148cac334'
);

DELETE FROM domain_events
WHERE application_id IN (
  'a0cca0dc-2fad-4286-b380-4585e8a88bc0',
  '323b4ae1-d63e-4af8-9f5e-010148cac334'
);

DELETE FROM cas_2_applications
WHERE id IN (
  'a0cca0dc-2fad-4286-b380-4585e8a88bc0',
  '323b4ae1-d63e-4af8-9f5e-010148cac334'
);

COMMIT;
