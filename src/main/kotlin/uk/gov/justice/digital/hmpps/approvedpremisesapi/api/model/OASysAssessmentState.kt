package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: completed,incomplete
*/
enum class OASysAssessmentState(val value: kotlin.String) {

  @JsonProperty("Completed")
  completed("Completed"),

  @JsonProperty("Incomplete")
  incomplete("Incomplete"),
}
