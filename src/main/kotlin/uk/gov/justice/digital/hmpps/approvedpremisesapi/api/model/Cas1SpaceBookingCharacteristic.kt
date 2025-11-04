package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1SpaceBookingCharacteristic(@get:JsonValue val value: kotlin.String) {

  HAS_EN_SUITE("hasEnSuite"),
  IS_ARSON_SUITABLE("isArsonSuitable"),
  IS_SINGLE("isSingle"),
  IS_STEP_FREE_DESIGNATED("isStepFreeDesignated"),
  IS_SUITED_FOR_SEX_OFFENDERS("isSuitedForSexOffenders"),
  IS_WHEELCHAIR_DESIGNATED("isWheelchairDesignated"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1SpaceBookingCharacteristic = values().first { it -> it.value == value }
  }
}
