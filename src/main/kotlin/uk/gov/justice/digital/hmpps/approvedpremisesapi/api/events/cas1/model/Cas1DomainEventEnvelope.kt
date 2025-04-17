package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import java.time.Instant
import java.util.UUID

interface Cas1DomainEventEnvelope<D> {
  val id: UUID
  val timestamp: Instant
  val eventType: EventType
  val eventDetails: D
}
