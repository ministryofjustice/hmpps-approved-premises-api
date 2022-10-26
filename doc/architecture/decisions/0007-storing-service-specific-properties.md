# 7. storing-service-specific-properties

Date: 2022-10-26

## Status

Accepted

## Context

Whilst many of the properties of a Premises are shared between CAS1 (Approved Premises) 
and CAS3 (Temporary Accommodation) there are some known divergences (e.g. Ap Code being
specific to CAS1).  There are also likely to be unknown divergences.

This requires us to have a way of sharing most properties but allowing for service specific 
properties too.

## Decision

- We will use JPA's inheritance support[1] to create a common base class (PremisesEntity) from 
  which a class for each service (`ApprovedPremisesEntity` and `TemporaryAccommodationPremisesEntity`) 
  will derive.
- We will use the `JOINED` strategy to model this in the database (i.e. one `premises` table storing 
  the common fields with an additional table for each of the derived classes `approved_premises` and 
  `temporary_accommodation_premises`) to store their specialised fields.
  - There is a performance impact from using this approach as JPA must generate a JOIN instead of 
    a single table select.  It's unlikely that this will be significant enough to cause an issue at 
    our anticipated level of load.
  - The `SINGLE_TABLE` approach was also considered but decided against as:
    - The properties of derived classes must be nullable in the table, but potentially not-nullable in 
      the Kotlin entity models.  Consistency between the two is preferable.
    - A single table with mixed fields becomes harder to reason about over time
  - The `TABLE_PER_CLASS` approach was also considered but decided against as:
    - Database level referential integrity must be abandoned for tables that currently have foreign keys 
      to the `premises` table as it's not possible to have a foreign key that references more than one table
    - Maintenance becomes harder (e.g. forgetting to add a new shared property to one of the tables)
- The existing `service` field will be used as the discriminator value (`CAS1` or `CAS3`) by which JPA knows 
  which type of Premises to return for a given ID

[1] [Overview of JPA Inheritance support](https://thorben-janssen.com/complete-guide-inheritance-strategies-jpa-hibernate)

## Consequences

- Adding new service specific properties is straight forward
- Determining the type of Premises (and switching behaviour as appropriate) can be done by type assertion
  ```
    when (premisesEntity) {
      is ApprovedPremisesEntity -> cas1SpecificBehaviour()
      is TemporaryAccommodationPremisesEntity -> cas3SpecificBehaviour()
      else -> unsupportedPremisesType()
    }
  ```
- Tests must use either `ApprovedPremisesEntityFactory` or `TemporaryAccommodationPremisesEntityFactory` to
  produce one of the concrete types rather than the previously used `PremisesEntityFactory` which covered both
