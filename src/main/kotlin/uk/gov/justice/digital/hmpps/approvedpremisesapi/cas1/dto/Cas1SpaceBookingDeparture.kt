package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId

data class Cas1SpaceBookingDeparture(

  @get:JsonProperty("reason", required = true) val reason: NamedId,

  @get:JsonProperty("parentReason") val parentReason: NamedId? = null,

  @get:JsonProperty("moveOnCategory") val moveOnCategory: NamedId? = null,

  @get:JsonProperty("notes") val notes: String? = null,
)
