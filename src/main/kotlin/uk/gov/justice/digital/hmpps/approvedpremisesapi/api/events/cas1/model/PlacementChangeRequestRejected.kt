package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.UUID

data class PlacementChangeRequestRejected(
  val changeRequestId: UUID,
  val changeRequestType: EventChangeRequestType,
  val booking: EventBookingSummary,
  val rejectedBy: StaffMember,
  val reason: Cas1DomainEventCodedId,
) : Cas1DomainEventPayload