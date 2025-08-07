package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType

data class CAS3BedspaceUnarchiveEvent(
  val eventDetails: CAS3BedspaceUnarchiveEventDetails,

  override val id: java.util.UUID,

  override val timestamp: java.time.Instant,

  override val eventType: EventType,
) : CAS3Event

data class CAS3BedspaceUnarchiveEventDetails(

  val bedspaceId: java.util.UUID,

  val premisesId: java.util.UUID,

  val userId: java.util.UUID,

  val currentStartDate: java.time.LocalDate,

  val currentEndDate: java.time.LocalDate,

  val newStartDate: java.time.LocalDate,
)
