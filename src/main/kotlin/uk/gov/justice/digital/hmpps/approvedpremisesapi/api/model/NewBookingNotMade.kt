package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewBookingNotMade(

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
