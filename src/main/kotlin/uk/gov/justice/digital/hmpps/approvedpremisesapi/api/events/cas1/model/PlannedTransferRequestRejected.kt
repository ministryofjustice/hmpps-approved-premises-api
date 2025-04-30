package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.LocalDate
import java.util.UUID

data class PlannedTransferRequestRejected(
  val changeRequestId: UUID,
  val booking: EventBookingSummary,
  val rejectedBy: StaffMember,
  val reason: Cas1DomainEventCodedId,
) : Cas1DomainEventPayload