package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId

data class Cas1SpaceBookingNonArrival(

  @get:JsonProperty("confirmedAt") val confirmedAt: java.time.Instant? = null,

  @get:JsonProperty("reason") val reason: NamedId? = null,

  @get:JsonProperty("notes") val notes: String? = null,
)
