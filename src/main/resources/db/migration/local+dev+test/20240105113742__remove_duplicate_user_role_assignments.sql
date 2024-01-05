-- Duplicates have been inserted into dev. Before we can add a database
-- constraint we need to remove these. The create_dev_users migration will put
-- them back.

DELETE FROM user_role_assignments
WHERE user_id IN (
  SELECT id FROM users WHERE delius_username IN (
    'JIMSNOWLDAP',
    'APPROVEDPREMISESTESTUSER',
    'TEMPORARY-ACCOMMODATION-E2E-TESTER',
    'TESTER.TESTY',
    'BERNARD.BEAKS',
    'PANESAR.JASPAL',
    'CAS-LOAD-TESTER'
    )
);
