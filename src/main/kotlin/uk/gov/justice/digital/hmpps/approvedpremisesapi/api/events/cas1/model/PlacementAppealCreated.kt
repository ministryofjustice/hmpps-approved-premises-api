package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

data class PlacementAppealCreated(
  val booking: EventBookingSummary,
  val requestedBy: StaffMember,
  val reason: Cas1DomainEventCodedId,
) : Cas1DomainEventPayload