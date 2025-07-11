package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@SuppressWarnings("EnumNaming", "ExplicitItLambdaParameter")
enum class Cas3BookingStatus(@get:JsonValue val value: String) {

  arrived("arrived"),
  notMinusArrived("notMinusArrived"),
  departed("departed"),
  cancelled("cancelled"),
  provisional("provisional"),
  confirmed("confirmed"),
  closed("closed"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas3BookingStatus = values().first { it -> it.value == value }
  }
}
