# How to clear a cache remotely

## Run book

To clear a cache on a non-local environment:

- Ensure nobody is deploying a change (or is going to deploy a change shortly.)

- Run `kubectl get pod` against the namespace for the environment you wish to run the seed job in. 

- Copy the name of one of the running `hmpps-approved-premises-api` pods.

- Log in to the pod in order to run the script, e.g. 
  ```
  kubectl exec --stdin --tty {pod name} -- /bin/bash
  ```

- Run the helper script from within the container to trigger the cache clear:
  ```
  /app/clear_cache {cache name}
  ```

  Where `cache name` is a value from the `CacheType` enum in the OpenAPI spec.  e.g.
  ```
  kubectl exec --stdin --tty {pod name} -- /bin/bash
  /app/clear_cache qCodeStaffMembers
  ```

- Check the logs via `kubectl logs {pod name}` to confirm the cache was cleared.
