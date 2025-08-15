package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class CAS3PremisesUnarchiveEvent(
  val eventDetails: CAS3PremisesUnarchiveEventDetails,

  override val id: UUID,

  override val timestamp: Instant,

  override val eventType: EventType,
) : CAS3Event

data class CAS3PremisesUnarchiveEventDetails(

  val premisesId: UUID,

  val userId: UUID,

  val currentStartDate: LocalDate,

  val newStartDate: LocalDate,

  val currentEndDate: LocalDate,

)
