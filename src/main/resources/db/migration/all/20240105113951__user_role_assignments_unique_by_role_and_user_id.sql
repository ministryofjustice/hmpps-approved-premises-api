ALTER TABLE user_role_assignments
ADD CONSTRAINT unique_role_user_constraint
UNIQUE (role, user_id);
