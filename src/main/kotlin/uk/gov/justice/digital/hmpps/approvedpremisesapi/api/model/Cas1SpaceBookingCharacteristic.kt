package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
* All of the characteristics applicable to a space booking
* Values: HAS_EN_SUITE,IS_ARSON_SUITABLE,IS_SINGLE,IS_STEP_FREE_DESIGNATED,IS_SUITED_FOR_SEX_OFFENDERS,IS_WHEELCHAIR_DESIGNATED
*/
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
    fun forValue(value: kotlin.String): Cas1SpaceBookingCharacteristic = entries.first { it.value == value }
  }
}
