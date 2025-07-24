package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

interface CAS3Event {
  val id: java.util.UUID

  val timestamp: java.time.Instant

  val eventType: EventType
}
