package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1SpaceBookingDeparture(

  val reason: NamedId,

  val parentReason: NamedId? = null,

  val moveOnCategory: NamedId? = null,

  val notes: kotlin.String? = null,
)
