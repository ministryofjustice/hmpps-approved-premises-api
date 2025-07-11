package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: approvedPremises,cas2,cas2v2,temporaryAccommodation
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ServiceName(@get:JsonValue val value: kotlin.String) {

  approvedPremises("approved-premises"),
  cas2("cas2"),
  cas2v2("cas2v2"),
  temporaryAccommodation("temporary-accommodation"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ServiceName = values().first { it -> it.value == value }
  }
}
