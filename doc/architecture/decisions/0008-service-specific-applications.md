# 8. service-specific-applications

Date: 2022-11-10

## Status

Proposed

## Context

The CAS3 (Temporary Accommodation) team need to collect personal data for people in prison or on
probation during the booking process.
While some properties of this data are the same as those captured by the CAS1 (Approved Premises)
team, there are some differences known as of writing
(e.g. PIPE applications being specific to CAS1), as well as the potential for unknown differences
to surface in the future.

## Decision

- CAS3 will not use the `bookings` table to store personal information.
  This aligns with CAS1's use of the `applications` table to store such data.
- We will split applications into service-specific tables using the same method as decided in
  [ADR 7](0007-storing-service-specific-properties.md) for premises:
  - We will use JPA's inheritance support to create a base class (`ApplicationEntity`) from which
    a class for each service will derive
    (`ApprovedPremisesApplicationEntity` and `TemporaryAccommodationApplicationEntity`).
  - We will use the `JOINED` strategy to model this in the database.
    The base table `applications` will contain the common fields, and derived tables
    (`approved_premises_applications` and `temporary_accommodation_applications`)
    will be linked to the base table and contain the service-specific data.
- We will update the `bookings` table to contain a nullable foreign key to the `applications`
  table.
  CAS3 requires the personal data to be linked to the booking, but CAS1 does not yet have such a
  requirement (but may do so in future), and so this approach minimises unnecessary disruption from
  breaking changes.
- Provisionally, CAS3 will automatically create an application at the same time as a booking is
  created.
  This is so that CAS3 can start capturing personal data as soon as possible, but will likely be
  superseded by dedicated application creation and management flows in the future.
- We will not create a `persons` (or similar) table.
  As the personal data is primarily needed for reporting, it's better to have a snapshot of this
  data at the time of recording.
  If we were to create a `persons` table, we would have the issue that changes to personal data
  (such as a change of name or gender) would affect past applications, unless we also created
  historical tables to capture when data changes.
  As neither CAS1 nor CAS3 have any intention of being a canonical source of personal data
  (this role belongs to Delius), this would be a complex solution with little additional benefit
  beyond storing this data as part of the application.

## Consequences

- Adding new service-specific properties to an application is straightforward.
- Determining the type of application can be done with the aid of the type system:
  ```kotlin
  when (applicationEntity) {
    is ApprovedPremisesApplicationEntity -> cas1SpecificBehaviour()
    is TemporaryAccommodationApplicationEntity -> cas3SpecificBehaviour()
    else -> unsupportedApplicationType()
  }
  ```
- Tests will need to be refactored to use either `ApprovedPremisesApplicationEntityFactory` or
  `TemporaryAccommodationApplicationEntityFactory` to produce an instance of an application, rather
  than `ApplicationEntityFactory`.
- This approach does not use any type- or database-level enforcement of the service-specific
  behaviour of linking the booking to the application.
  While this should be handled by the decision to automatically create an application at the same
  time as the booking in CAS3, we will need to take care to make sure that this behaves as expected
  through suitable testing.
