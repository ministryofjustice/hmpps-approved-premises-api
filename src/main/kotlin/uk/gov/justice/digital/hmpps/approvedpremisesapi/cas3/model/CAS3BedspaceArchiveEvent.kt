package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType

data class CAS3BedspaceArchiveEvent(
  val eventDetails: CAS3BedspaceArchiveEventDetails,

  override val id: java.util.UUID,

  override val timestamp: java.time.Instant,

  override val eventType: EventType,
) : CAS3Event

data class CAS3BedspaceArchiveEventDetails(

  val bedspaceId: java.util.UUID,

  val premisesId: java.util.UUID,

  val endDate: java.time.LocalDate,

  val userId: java.util.UUID,

)
