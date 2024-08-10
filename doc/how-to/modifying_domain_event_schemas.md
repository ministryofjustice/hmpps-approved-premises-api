# Modifying Domain Event JSON

## Definition and Usage

Each domain event type has a JSON schema defined in the domain-events-api.yml file. For example, the 'APPROVED_PREMISES_BOOKING_CANCELLED' type has a 'BookingCancelledEnvelope' defined in the domain-events-api.yml file.

When a domain event is generated its JSON is persisted to domain_events.data. This JSON is then used for the following:

1. Consumed by the probation-integration service(s) to retrieve detail about a domain event published to the SQS Queue, via the endpoints defined in domain-events-api.yml
2. Used by the CAS1 DomainEventDescriber component to produce timeline descriptions for domain events

## Considerings when making changes

If we need to modify a domain event's schema, we need to be mindful that any currently persisted domain event json (i.e. in domain_events.data) will not necessarily conform to this new schema. This may cause issues in the aforementioned use cases because they will deserialize the JSON into a generated Kotlin object built against the latest version of the schema. If the 'legacy' json isn't compatible with this object model, an exception will occur.

For example, if a new mandatory field is added to a domain event's schema, any existing domain event JSON will no longer be readable

Although if will differ on a case-by-case basis, it's only possible to make a change to a domain event schema if existing domain events can be adapted to satisfy the new schema. If they can't, we have to consider adding a new domain event type (e.g. APPROVED_PREMISES_BOOKING_CHANGED_V2). This should be avoided if possible because it will create additional work in both CAS API and receivers of the domain events (probation-integration).

## Making a schema change that is backwards-compatible

Unit and Integration tests will automatically pick up a new schema version defined in DomainEventEntity.kt and test serialization and de-serialization. To enable this when adding a new schema version:

1. Add a new schema version entry to the corresponding DomainEventType (in DomainEventEntity.kt)
2. Update Cas1DomainEventFactory.kt to ensure that JSON provided for prior schema versions reflect the legacy JSON (see createCas1DomainEventEnvelopeForSchemaVersion for an example of this)

## Managing a change that is not backwards-compatible

In addition to the changes mentioned in the prior section:

1. Update the CAS-specific DomainEventService to adapt domain event json created against the older schema version to the new schema. For an example of this, see Cas1DomainEventMigrationService
