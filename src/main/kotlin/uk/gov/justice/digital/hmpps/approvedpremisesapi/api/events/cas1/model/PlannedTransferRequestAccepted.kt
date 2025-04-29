package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.UUID

data class PlannedTransferRequestAccepted(
  val changeRequestId: UUID,
  val acceptedBy: StaffMember,
  val from: EventBookingSummary,
  val to: EventBookingSummary,
) : Cas1DomainEventPayload