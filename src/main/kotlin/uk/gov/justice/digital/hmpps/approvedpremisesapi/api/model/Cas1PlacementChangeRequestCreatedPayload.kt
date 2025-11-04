package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1PlacementChangeRequestCreatedPayload(

  val changeRequestId: java.util.UUID,

  val booking: Cas1TimelineEventPayloadBookingSummary,

  val reason: NamedId,

  val changeRequestType: Cas1ChangeRequestType,

  override val type: Cas1TimelineEventType,
) : Cas1TimelineEventContentPayload
