package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PlacementCriteria(@get:JsonValue val value: kotlin.String) {

  isPIPE("isPIPE"),
  isESAP("isESAP"),
  isMHAPStJosephs("isMHAPStJosephs"),
  isMHAPElliottHouse("isMHAPElliottHouse"),
  isSemiSpecialistMentalHealth("isSemiSpecialistMentalHealth"),
  isRecoveryFocussed("isRecoveryFocussed"),
  hasBrailleSignage("hasBrailleSignage"),
  hasTactileFlooring("hasTactileFlooring"),
  hasHearingLoop("hasHearingLoop"),
  isStepFreeDesignated("isStepFreeDesignated"),
  isArsonDesignated("isArsonDesignated"),
  isWheelchairDesignated("isWheelchairDesignated"),
  isSingle("isSingle"),
  isCatered("isCatered"),
  isSuitedForSexOffenders("isSuitedForSexOffenders"),
  isSuitableForVulnerable("isSuitableForVulnerable"),
  acceptsSexOffenders("acceptsSexOffenders"),
  acceptsHateCrimeOffenders("acceptsHateCrimeOffenders"),
  acceptsChildSexOffenders("acceptsChildSexOffenders"),
  acceptsNonSexualChildOffenders("acceptsNonSexualChildOffenders"),
  isArsonSuitable("isArsonSuitable"),
  isGroundFloor("isGroundFloor"),
  hasEnSuite("hasEnSuite"),
  arsonOffences("arsonOffences"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PlacementCriteria = values().first { it -> it.value == value }
  }
}
