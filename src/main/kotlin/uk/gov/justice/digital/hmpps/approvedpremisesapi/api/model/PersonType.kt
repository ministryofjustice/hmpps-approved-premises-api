package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: fullPerson,restrictedPerson,unknownPerson
*/
enum class PersonType(val value: kotlin.String) {

  @JsonProperty("FullPerson")
  fullPerson("FullPerson"),

  @JsonProperty("RestrictedPerson")
  restrictedPerson("RestrictedPerson"),

  @JsonProperty("UnknownPerson")
  unknownPerson("UnknownPerson"),
}
