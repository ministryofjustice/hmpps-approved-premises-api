# How to run a migration job remotely

To run a migration job against a non-local environment:

- Ensure nobody is deploying a change (or is going to deploy a change shortly.)

- Set target namespace as a variable, updating the env as required
  ```
  env=preprod
  namespace=hmpps-community-accommodation-$env
  ```

- Run against the namespace for the environment you wish to run the seed job in, e.g:
  ```
  kubectl get pod -n $namespace
  ```
  
- Copy the name of one of the running `hmpps-approved-premises-api` pods or set it as a variable, e.g:
  ```
  pod={pod name}
  ```

- Run the helper script from within the container to trigger the migration job:
  ```
  kubectl exec -n $namespace --stdin --tty $pod -- /bin/bash
  /app/run_migration_job {job type}
  ```
  Where `job type` is a value from the `MigrationJobType` enum in the OpenAPI spec.  e.g.
  ```
  kubectl exec -n $namespace --stdin --tty $pod -- /bin/bash
  /app/run_migration_job update_all_users_from_community_api
  ```

- Check the logs using [Azure Application Insights](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4154196024/Viewing+and+Tailing+Kubernetes+logs) to see how processing is progressing. The following query will provide traces and exceptions for migration jobs:
  ```
  union traces, exceptions
  | where cloud_RoleName == 'approved-premises-api' and customDimensions contains ("uk.gov.justice.digital.hmpps.approvedpremisesapi.migration")
  | project timestamp, message, outerMessage, severityLevel, customDimensions, problemId, operation_Name, operation_Id
   ```

