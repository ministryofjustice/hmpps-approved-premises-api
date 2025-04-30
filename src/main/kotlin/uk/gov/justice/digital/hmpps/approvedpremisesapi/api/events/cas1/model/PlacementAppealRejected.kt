package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

data class PlacementAppealRejected(
  val booking: EventBookingSummary,
  val rejectedBy: StaffMember,
  val reason: Cas1DomainEventCodedId,
) : Cas1DomainEventPayload