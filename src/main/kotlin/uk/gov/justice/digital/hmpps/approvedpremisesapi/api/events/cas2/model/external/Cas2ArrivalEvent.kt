package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.external

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import java.time.Instant
import java.util.UUID

data class Cas2ArrivalEvent(
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
  val eventDetails: Cas2ArrivalEventDetails,
) : Cas2Event
