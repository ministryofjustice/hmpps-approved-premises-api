# Deprecations - Common Booking Entities

`bookings` and related tables are now exclusively used by cas3.

As part of this work new entities have been created to access this table, prefixed with CAS3 (e.g. `Cas3BookingEntity`, `Cas3ArrivalEntity` etc.)

For this reason the following 'common' booking Entities are now deprecated, and will be removed in the near future:

For this reason, the following entities are deprecated and will be removed in the near future:

* `ArrivalEntity`
* `CancellationEntity`
* `DateChangeEntity` (this doesn't have a cas3 equivalent and the table is now empty)
* `DepartureEntity`
* `ExtensionEntity`
* `NonArrivalEntity`

As part of this removal changes/removal of endpoints, seed jobs, migration jobs, services, repositories and factories will also be required