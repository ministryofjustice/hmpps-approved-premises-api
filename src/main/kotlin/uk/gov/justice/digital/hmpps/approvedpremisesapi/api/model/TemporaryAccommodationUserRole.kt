package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: assessor,referrer,reporter
*/
enum class TemporaryAccommodationUserRole(val value: kotlin.String) {

  @JsonProperty("assessor")
  assessor("assessor"),

  @JsonProperty("referrer")
  referrer("referrer"),

  @JsonProperty("reporter")
  reporter("reporter"),
}
