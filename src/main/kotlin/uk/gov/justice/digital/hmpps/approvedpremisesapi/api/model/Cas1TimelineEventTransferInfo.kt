package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1TimelineEventTransferInfo(

  val type: Cas1TimelineEventTransferType,

  val booking: Cas1TimelineEventPayloadBookingSummary,

  val changeRequestId: java.util.UUID? = null,
)
