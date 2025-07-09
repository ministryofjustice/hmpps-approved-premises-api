package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType

class CAS3BedspaceUnarchiveEvent(
  val eventDetails: CAS3BedspaceUnarchiveEventDetails,

  override val id: java.util.UUID,

  override val timestamp: java.time.Instant,

  override val eventType: EventType,
) : CAS3Event

data class CAS3BedspaceUnarchiveEventDetails(

  val bedspaceId: java.util.UUID,

  val userId: java.util.UUID,

  val currentStartDate: java.time.LocalDate,

  val currentEndDate: java.time.LocalDate,

  val newStartDate: java.time.LocalDate,
)
