package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.UUID

data class PlacementAppealRejected(
  val changeRequestId: UUID,
  val booking: EventBookingSummary,
  val rejectedBy: StaffMember,
  val reason: Cas1DomainEventCodedId,
) : Cas1DomainEventPayload