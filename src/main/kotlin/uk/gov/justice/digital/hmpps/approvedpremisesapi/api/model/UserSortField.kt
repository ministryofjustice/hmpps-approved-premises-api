package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: personName
*/
enum class UserSortField(val value: kotlin.String) {

  @JsonProperty("name")
  personName("name"),
}
