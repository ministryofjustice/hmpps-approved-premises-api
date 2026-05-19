package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateAssessment(

  @get:JsonProperty("data", required = true) val `data`: Map<String, Any>,

  @get:JsonProperty("releaseDate") val releaseDate: java.time.LocalDate? = null,

  @get:JsonProperty("accommodationRequiredFromDate") val accommodationRequiredFromDate: java.time.LocalDate? = null,
)
