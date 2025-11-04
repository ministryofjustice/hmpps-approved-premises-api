package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1ChangeRequest(

  val id: java.util.UUID,

  val type: Cas1ChangeRequestType,

  val createdAt: java.time.Instant,

  val requestReason: NamedId,

  val requestJson: kotlin.Any,

  val spaceBookingId: java.util.UUID,

  val updatedAt: java.time.Instant,

  val decision: Cas1ChangeRequestDecision? = null,

  val decisionJson: kotlin.Any? = null,

  val rejectionReason: NamedId? = null,
)
