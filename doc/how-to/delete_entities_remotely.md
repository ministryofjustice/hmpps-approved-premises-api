# How to delete an Entity remotely

To delete an Entity against a non-local environment:

- Run `kubectl get pod` against the namespace for the environment you wish to delete the Booking from.
- Copy the name of one of the running `hmpps-approved-premises-api` pods.
- Run the helper script from within the container to trigger the deletion:

  ```shell
  kubectl exec --stdin --ty {pod name} -- /app/hard_delete {entity type} {entity id}
  ```

The supported entity types are:

- Premises:

  ```sh
  # Deletes a premises
  kubectl exec --stdin --tty {pod name} -- /app/hard_delete premises {premises id}
  ```

- Rooms:

  ```sh
  # Deletes a room
  kubectl exec --stdin --tty {pod name} -- /app/hard_delete room {room id}
  ```

- Bookings:

  The service _used_ to allow hard deleting a booking during private beta as a
  quick way to remove problematic bookings. Since enabling domain domain events,
  where we communicate with ndelius about new and updated bookings, we can no
  longer offer hard deletion as a blunt tool. Ndelius must be informed as to the
  outcome of all bookings so they must be cancelled or managed through to
  departure as intended.

Note that if you're deleting either a premises or a room, it will fail if any bookings exist for beds
in that premises or room.
This is to avoid inadvertently losing operational data.
If such a premises or room really does need to be deleted, this script will first need to be called on
each of its bookings to delete them.

This doesn't apply to lost beds/voids, which are considered to be a less relevant indicator that the
premises or room is in use.
Any of these that exist will also be deleted along with the entity.
