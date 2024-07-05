UPDATE move_on_categories
SET is_active = false
WHERE name = 'Unknown (the probation practitioner has not added the move-on category to NDelius)'
  AND service_scope = 'temporary-accommodation';

INSERT INTO move_on_categories (id, name, is_active, service_scope, legacy_delius_category_code)
VALUES ('5dfd0cc4-8be3-4788-a7ba-a84d32efe5ea', 'Pending (no category has been added to NDelius)', true, 'temporary-accommodation', null)