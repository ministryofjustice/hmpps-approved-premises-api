# How to delete a Booking remotely

To delete a Booking against a non-local environment:

- Run `kubectl get pod` against the namespace for the environment you wish to delete the Booking from.
- Copy the name of one of the running `hmpps-approved-premises-api` pods.
- Run the helper script from within the container to trigger the deletion:
  ```
  kubectl exec --stdin --tty {pod name} -- /app/delete_booking {booking id}
  ```
