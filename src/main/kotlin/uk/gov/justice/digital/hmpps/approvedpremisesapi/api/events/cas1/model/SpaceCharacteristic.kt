package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class SpaceCharacteristic(@get:JsonValue val value: kotlin.String) {

  acceptsChildSexOffenders("acceptsChildSexOffenders"),
  acceptsHateCrimeOffenders("acceptsHateCrimeOffenders"),
  acceptsNonSexualChildOffenders("acceptsNonSexualChildOffenders"),
  acceptsSexOffenders("acceptsSexOffenders"),
  additionalRestrictions("additionalRestrictions"),
  hasArsonInsuranceConditions("hasArsonInsuranceConditions"),
  hasBrailleSignage("hasBrailleSignage"),
  hasCallForAssistance("hasCallForAssistance"),
  hasCrib7Bedding("hasCrib7Bedding"),
  hasEnSuite("hasEnSuite"),
  hasFixedMobilityAids("hasFixedMobilityAids"),
  hasHearingLoop("hasHearingLoop"),
  hasLift("hasLift"),
  hasNearbySprinkler("hasNearbySprinkler"),
  hasSmokeDetector("hasSmokeDetector"),
  hasStepFreeAccess("hasStepFreeAccess"),
  hasStepFreeAccessToCommunalAreas("hasStepFreeAccessToCommunalAreas"),
  hasTactileFlooring("hasTactileFlooring"),
  hasTurningSpace("hasTurningSpace"),
  hasWheelChairAccessibleBathrooms("hasWheelChairAccessibleBathrooms"),
  hasWideAccessToCommunalAreas("hasWideAccessToCommunalAreas"),
  hasWideDoor("hasWideDoor"),
  hasWideStepFreeAccess("hasWideStepFreeAccess"),
  isArsonDesignated("isArsonDesignated"),
  isArsonSuitable("isArsonSuitable"),
  isCatered("isCatered"),
  isESAP("isESAP"),
  isFullyFm("isFullyFm"),
  isGroundFloor("isGroundFloor"),
  isGroundFloorNrOffice("isGroundFloorNrOffice"),
  isIAP("isIAP"),
  isPIPE("isPIPE"),
  isRecoveryFocussed("isRecoveryFocussed"),
  isSemiSpecialistMentalHealth("isSemiSpecialistMentalHealth"),
  isSingle("isSingle"),
  isStepFreeDesignated("isStepFreeDesignated"),
  isSuitableForVulnerable("isSuitableForVulnerable"),
  isSuitedForSexOffenders("isSuitedForSexOffenders"),
  isTopFloorVulnerable("isTopFloorVulnerable"),
  isWheelchairAccessible("isWheelchairAccessible"),
  isWheelchairDesignated("isWheelchairDesignated"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): SpaceCharacteristic = values().first { it -> it.value == value }
  }
}
