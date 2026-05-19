package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PersonSummaryDiscriminator(@get:JsonValue val value: String) {

  fullPersonSummary("FullPersonSummary"),
  restrictedPersonSummary("RestrictedPersonSummary"),
  unknownPersonSummary("UnknownPersonSummary"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): PersonSummaryDiscriminator = values().first { it.value == value }
  }
}
