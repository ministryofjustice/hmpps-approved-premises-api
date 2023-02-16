# How to run a migration job remotely

To run a migration job against a non-local environment:

- Ensure nobody is deploying a change (or is going to deploy a change shortly.)
- Run `kubectl get pod` against the namespace for the environment you wish to run the seed job in.
- Copy the name of one of the running `hmpps-approved-premises-api` pods.
- Run the helper script from within the container to trigger the migration job:
  ```
  kubectl exec --stdin --tty {pod name} -- /bin/bash
  /app/run_migration_job {job type}
  ```
  Where `job type` is a value from the `MigrationJobType` enum in the OpenAPI spec.  e.g.
  ```
  kubectl exec --stdin --tty {pod name} -- /bin/bash
  /app/run_migration_job update_all_users_from_community_api
  ```
- Check the logs via `kubectl logs {pod name}` to see how processing is progressing.
