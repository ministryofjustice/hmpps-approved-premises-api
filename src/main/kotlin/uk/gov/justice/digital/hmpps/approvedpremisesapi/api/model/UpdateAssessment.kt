package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param &#x60;data&#x60;
 * @param releaseDate
 * @param accommodationRequiredFromDate
 */
data class UpdateAssessment(

  @get:JsonProperty("data", required = true) val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,

  @get:JsonProperty("releaseDate") val releaseDate: java.time.LocalDate? = null,

  @get:JsonProperty("accommodationRequiredFromDate") val accommodationRequiredFromDate: java.time.LocalDate? = null,
)
