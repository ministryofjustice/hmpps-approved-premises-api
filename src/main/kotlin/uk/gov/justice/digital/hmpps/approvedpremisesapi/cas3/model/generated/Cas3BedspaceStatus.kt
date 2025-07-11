package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas3BedspaceStatus(@get:JsonValue val value: String) {

  online("online"),
  archived("archived"),
  upcoming("upcoming"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas3BedspaceStatus = entries.first { it.value == value }
  }
}
