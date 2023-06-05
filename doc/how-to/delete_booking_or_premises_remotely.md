# How to delete a Booking or Premises remotely

To delete a Booking or Premises against a non-local environment:

- Run `kubectl get pod` against the namespace for the environment you wish to delete the Booking from.
- Copy the name of one of the running `hmpps-approved-premises-api` pods.
- Run the helper script from within the container to trigger the deletion:
  ```sh
  # Deletes a booking
  kubectl exec --stdin --tty {pod name} -- /app/hard_delete booking {booking id}
  # Deletes a premises
  kubectl exec --stdin --tty {pod name} -- /app/hard_delete premises {premises id}
  ```
