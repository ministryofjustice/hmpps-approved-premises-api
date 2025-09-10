package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class PremisesCharacteristic(
  val id: UUID,
  val name: String,
  val description: String,
)

data class Characteristic(
  val id: UUID,
  val name: String,
  val serviceScope: ServiceScope,
  val modelScope: ModelScope,
  val propertyName: String? = null,
) {
  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class ServiceScope(@get:JsonValue val value: String) {

    approvedMinusPremises("approved-premises"),
    temporaryMinusAccommodation("temporary-accommodation"),
    star("*"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: String): ServiceScope = entries.first { it.value == value }
    }
  }

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class ModelScope(@get:JsonValue val value: kotlin.String) {

    premises("premises"),
    room("room"),
    star("*"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: String): ModelScope = entries.first { it.value == value }
    }
  }
}
