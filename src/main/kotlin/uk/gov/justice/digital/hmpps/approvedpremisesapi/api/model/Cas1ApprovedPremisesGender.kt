package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: man,woman
*/
enum class Cas1ApprovedPremisesGender(val value: kotlin.String) {

  @JsonProperty("man")
  man("man"),

  @JsonProperty("woman")
  woman("woman"),
}
