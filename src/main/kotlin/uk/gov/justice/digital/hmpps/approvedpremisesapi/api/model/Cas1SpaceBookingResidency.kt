package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1SpaceBookingResidency(@get:JsonValue val value: String) {

  upcoming("upcoming"),
  current("current"),
  historic("historic"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas1SpaceBookingResidency = values().first { it.value == value }
  }
}
