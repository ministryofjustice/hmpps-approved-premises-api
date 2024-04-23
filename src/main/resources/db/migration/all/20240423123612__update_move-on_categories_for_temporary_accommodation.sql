UPDATE move_on_categories
SET name = 'They’re deceased'
WHERE name = 'N/A (only to be used where person on probation is deceased)'
  AND service_scope = 'temporary-accommodation';

UPDATE move_on_categories
SET name = 'Another CAS3 property or bedspace'
WHERE name = 'N/A (only to be used where person on probation has moved to another CAS3 property)'
  AND service_scope = 'temporary-accommodation';

INSERT INTO move_on_categories(id, name, is_active, service_scope, legacy_delius_category_code)
VALUES
    ('ab35e40c-17c3-460b-9986-16352e1286a9', 'Unknown (the probation practitioner has not added the move-on category to NDelius)', TRUE, 'temporary-accommodation', NULL),
    ('4874c13a-fced-4d73-869d-1ac0eff45f66', 'They’re unlawfully at large or at large', TRUE, 'temporary-accommodation', NULL);