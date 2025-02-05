# 26. Prison Transfers

Date: 2025-02-05

## Status

Accepted

## Context

Currently, when a prisoner is transferred to another prison and assigned a new POM, if there is an application that has
been submitted this is not transferred to the new prison, thus meaning that the new prison never receives the original
application.

The old applications are never assigned to the new POM and stay with the old POM because the frontend has no way of
distinguishing which application is transferred.

We need to keep a record of the prisoners transfer, both when the prisoner moves prisons and when the prisoner is
assigned a new POM after the move.

## Options

In both cases we listen to the domain events prisoner-offender-search.prisoner.updated (to get the location transfer)
and offender-management.allocation.changed (to get the new pom allocation).

We need to store this info, and we also need to be able to show this info in the application timeline belonging to that
prisoner.

1) We store it in the already created domain events table. This is beneficial because the table already exists and
   already percolates changes to the application timeline. However, this table was created to store emitted domain
   events, not received domain events. Also, some extra manipulation would need to be done to the data when it comes out
   of the domain events table and is sent with the application to the frontend.
2) We create a brand-new table called prisoner locations that stores the bespoke info we need (prisonCode and staffId).
   We would need to set up this table to percolate to the application timeline, however it would be easy to package this
   data
   with the applications when it is sent to the frontend.

## Decision

Go with Option 2

* Create a new table called prisoner locations that is bespoke to the data that we need to show to the frontend and
  package that info with the applications when they are sent to the frontend and also show it in the application
  timeline
* This separates the prisoner locations from the domain events which makes sense as it is separate info that is not
  related to the emitting of domain events
* Extra work to percolate this new table to the timeline is minimal and other tables already do this (status table)
* It will be easy and clean to get the data out of the prisoner locations table as we can filter just on application id,
  and we can find the current location by checking that the endDate on the row for that application is null

## Caveats

This is a new feature and will slowly be implemented over the coming weeks.

This can possibly be broken until we start listening too and processing offender-merged events. If the NOMS number
changes before a transfer, we won't be able to update the location.

## Consequences

New filtering will be needed from the frontend on the get applications api which will allow filtering by applications
that have never moved, that were transferred out and that were transferred in.

Also, the API will need to use the data from the prisoner location table, rather than what's stored on the
application: 'created_by_user' and 'prison_code' now become what they say - the user that created the application, but
not necessarily the allocated user. We might need to rename the column to reflect this