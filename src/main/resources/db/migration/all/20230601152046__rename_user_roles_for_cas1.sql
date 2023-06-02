UPDATE user_role_assignments
SET role = 'CAS1_APPLICANT'
WHERE role = 'APPLICANT';

UPDATE user_role_assignments
SET role = 'CAS1_ASSESSOR'
WHERE role = 'ASSESSOR';

UPDATE user_role_assignments
SET role = 'CAS1_MANAGER'
WHERE role = 'MANAGER';

UPDATE user_role_assignments
SET role = 'CAS1_MATCHER'
WHERE role = 'MATCHER';

UPDATE user_role_assignments
SET role = 'CAS1_ADMIN'
WHERE role = 'ROLE_ADMIN';

UPDATE user_role_assignments
SET role = 'CAS1_WORKFLOW_MANAGER'
WHERE role = 'WORKFLOW_MANAGER';
