package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.UUID

data class PlacementChangeRequestAccepted(
  val changeRequestId: UUID,
  val changeRequestType: EventChangeRequestType,
  val booking: EventBookingSummary,
  val acceptedBy: StaffMember,
) : Cas1DomainEventPayload
