package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
@SuppressWarnings("EnumNaming", "ExplicitItLambdaParameter")
enum class Cas3BedspaceStatus(@get:JsonValue val value: String) {

  online("online"),
  archived("archived"),
  upcoming("upcoming"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas3BedspaceStatus = values().first { it -> it.value == value }
  }
}
