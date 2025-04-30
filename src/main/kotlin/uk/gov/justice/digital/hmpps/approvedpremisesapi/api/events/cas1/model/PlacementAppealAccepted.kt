package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

data class PlacementAppealAccepted(
  val booking: EventBookingSummary,
  val acceptedBy: StaffMember,
) : Cas1DomainEventPayload