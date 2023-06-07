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

Note that if you're deleting a premises, it will fail if any bookings exist for beds in that premises.
This is to avoid inadvertently losing operational data.
If such a premises really does need to be deleted, this script will first need to be called on each of
its bookings to delete them.

This doesn't apply to lost beds/voids, which are considered to be a less relevant indicator that the
premises is in use.
Any of these that exist will also be deleted along with the premises.
