package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: upcoming,current,historic
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1SpaceBookingResidency(@get:JsonValue val value: kotlin.String) {

  upcoming("upcoming"),
  current("current"),
  historic("historic"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1SpaceBookingResidency = values().first { it -> it.value == value }
  }
}
