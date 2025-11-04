package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PersonReference(

  @get:JsonProperty("noms", required = true) val noms: kotlin.String,

  @get:JsonProperty("crn") val crn: kotlin.String? = null,
)
