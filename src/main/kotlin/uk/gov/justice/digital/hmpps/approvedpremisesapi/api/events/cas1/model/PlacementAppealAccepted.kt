package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.UUID

data class PlacementAppealAccepted(
  val changeRequestId: UUID,
  val booking: EventBookingSummary,
  val acceptedBy: StaffMember,
) : Cas1DomainEventPayload