package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

interface CAS3Event {
  val id: java.util.UUID

  val timestamp: java.time.Instant

  val eventType: EventType
}
