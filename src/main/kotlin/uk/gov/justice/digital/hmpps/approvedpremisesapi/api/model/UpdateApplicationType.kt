package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: CAS1,CAS2,CAS3
*/
enum class UpdateApplicationType(val value: kotlin.String) {

  @JsonProperty("CAS1")
  CAS1("CAS1"),

  @JsonProperty("CAS2")
  CAS2("CAS2"),

  @JsonProperty("CAS3")
  CAS3("CAS3"),
}
