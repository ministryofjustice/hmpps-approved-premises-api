# How to run a seed job remotely

To process a seed CSV against a non-local environment:

- Ensure nobody is deploying a change (or is going to deploy a change shortly.)
- Run `kubectl get pod` against the namespace for the environment you wish to run the seed job in.
- Copy the name of one of the running `hmpps-approved-premises-api` pods.
- Transfer the CSV to the `/tmp/seed` directory in the container via kubectl, e.g.
  ```
  kubectl cp /local/path/ap_seed_file.csv {pod name}:/tmp/seed/ap_seed_file.csv
  ```
- Run the helper script from within the container to trigger the seed job:
  ```
  kubectl exec --stdin --tty {pod name} -- /bin/bash
  /app/script/run_seed_job {seed type} {file name without extension}
  ```
  Where `seed type` is a value from the `SeedFileType` enum in the OpenAPI spec.  e.g.
  ```
  kubectl exec --stdin --tty {pod name} -- /bin/bash
  /app/script/run_seed_job approved_premises ap_seed_file
  ```
- Check the logs via `kubectl logs {pod name}` to see how processing is progressing.
