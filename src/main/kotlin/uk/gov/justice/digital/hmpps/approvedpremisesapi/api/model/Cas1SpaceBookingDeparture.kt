package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1SpaceBookingDeparture(

  @get:JsonProperty("reason", required = true) val reason: NamedId,

  @get:JsonProperty("parentReason") val parentReason: NamedId? = null,

  @get:JsonProperty("moveOnCategory") val moveOnCategory: NamedId? = null,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
