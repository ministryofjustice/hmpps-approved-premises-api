package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CAS3BedspaceUnarchiveEvent(
  val eventDetails: CAS3BedspaceUnarchiveEventDetails,
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
) : CAS3Event

data class CAS3BedspaceUnarchiveEventDetails(
  val bedspaceId: UUID,
  val premisesId: UUID,
  val currentStartDate: LocalDate,
  val currentEndDate: LocalDate,
  val newStartDate: LocalDate,
  val userId: UUID,
  val transactionId: UUID?,
)
