package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param &#x60;data&#x60;
 * @param releaseDate
 * @param accommodationRequiredFromDate
 */
data class UpdateAssessment(

  val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,

  val releaseDate: java.time.LocalDate? = null,

  val accommodationRequiredFromDate: java.time.LocalDate? = null,
)
