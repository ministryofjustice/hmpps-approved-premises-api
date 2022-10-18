ALTER TABLE probation_officers RENAME TO "users";
ALTER TABLE applications RENAME COLUMN created_by_probation_officer_id TO created_by_user_id;
ALTER TABLE assessments RENAME COLUMN allocated_to_probation_officer_id TO allocated_to_user_id;
