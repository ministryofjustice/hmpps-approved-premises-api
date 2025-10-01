package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CAS3BedspaceArchiveEvent(
  val eventDetails: CAS3BedspaceArchiveEventDetails,
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
) : CAS3Event

data class CAS3BedspaceArchiveEventDetails(
  val bedspaceId: UUID,
  val premisesId: UUID,
  val endDate: LocalDate,
  val currentEndDate: LocalDate?,
  val userId: UUID,
  val transactionId: UUID?,
)
