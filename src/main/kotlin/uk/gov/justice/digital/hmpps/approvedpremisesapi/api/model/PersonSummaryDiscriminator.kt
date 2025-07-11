package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: fullPersonSummary,restrictedPersonSummary,unknownPersonSummary
*/
enum class PersonSummaryDiscriminator(@get:JsonValue val value: kotlin.String) {

  fullPersonSummary("FullPersonSummary"),
  restrictedPersonSummary("RestrictedPersonSummary"),
  unknownPersonSummary("UnknownPersonSummary"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PersonSummaryDiscriminator = values().first { it -> it.value == value }
  }
}
