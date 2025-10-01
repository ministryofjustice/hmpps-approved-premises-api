package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class CAS3PremisesArchiveEvent(
  val eventDetails: CAS3PremisesArchiveEventDetails,
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
) : CAS3Event

data class CAS3PremisesArchiveEventDetails(
  val premisesId: UUID,
  val endDate: LocalDate,
  val userId: UUID,
  val transactionId: UUID?,
)
