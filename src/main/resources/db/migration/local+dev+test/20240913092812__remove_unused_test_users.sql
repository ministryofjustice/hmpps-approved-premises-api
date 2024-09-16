-- ('695ba399-c407-4b66-aafc-e8835d72b8a7', 'Bernard Beaks', 'bernard.beaks', 2500057096),
-- ('045b71d3-9845-49b3-a79b-c7799a6bc7bc', 'Panesar Jaspal', 'panesar.jaspal', 2500054544);
DELETE FROM user_role_assignments WHERE user_id IN ('695ba399-c407-4b66-aafc-e8835d72b8a7','045b71d3-9845-49b3-a79b-c7799a6bc7bc');
DELETE FROM users WHERE id IN ('695ba399-c407-4b66-aafc-e8835d72b8a7','045b71d3-9845-49b3-a79b-c7799a6bc7bc');