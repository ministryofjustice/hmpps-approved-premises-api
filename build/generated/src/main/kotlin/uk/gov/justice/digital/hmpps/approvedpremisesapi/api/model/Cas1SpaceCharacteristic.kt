package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* All of the characteristics of both premises and rooms
* Values: acceptsChildSexOffenders,acceptsHateCrimeOffenders,acceptsNonSexualChildOffenders,acceptsSexOffenders,hasArsonInsuranceConditions,hasBrailleSignage,hasCallForAssistance,hasCrib7Bedding,hasEnSuite,hasFixedMobilityAids,hasHearingLoop,hasLift,hasNearbySprinkler,hasSmokeDetector,hasStepFreeAccess,hasStepFreeAccessToCommunalAreas,hasTactileFlooring,hasTurningSpace,hasWheelChairAccessibleBathrooms,hasWideAccessToCommunalAreas,hasWideDoor,hasWideStepFreeAccess,isArsonDesignated,isArsonSuitable,isCatered,isFullyFm,isGroundFloor,isGroundFloorNrOffice,isIAP,isSingle,isStepFreeDesignated,isSuitableForVulnerable,isSuitedForSexOffenders,isTopFloorVulnerable,isWheelchairAccessible,isWheelchairDesignated
*/
enum class Cas1SpaceCharacteristic(val value: kotlin.String) {

    @JsonProperty("acceptsChildSexOffenders") acceptsChildSexOffenders("acceptsChildSexOffenders"),
    @JsonProperty("acceptsHateCrimeOffenders") acceptsHateCrimeOffenders("acceptsHateCrimeOffenders"),
    @JsonProperty("acceptsNonSexualChildOffenders") acceptsNonSexualChildOffenders("acceptsNonSexualChildOffenders"),
    @JsonProperty("acceptsSexOffenders") acceptsSexOffenders("acceptsSexOffenders"),
    @JsonProperty("hasArsonInsuranceConditions") hasArsonInsuranceConditions("hasArsonInsuranceConditions"),
    @JsonProperty("hasBrailleSignage") hasBrailleSignage("hasBrailleSignage"),
    @JsonProperty("hasCallForAssistance") hasCallForAssistance("hasCallForAssistance"),
    @JsonProperty("hasCrib7Bedding") hasCrib7Bedding("hasCrib7Bedding"),
    @JsonProperty("hasEnSuite") hasEnSuite("hasEnSuite"),
    @JsonProperty("hasFixedMobilityAids") hasFixedMobilityAids("hasFixedMobilityAids"),
    @JsonProperty("hasHearingLoop") hasHearingLoop("hasHearingLoop"),
    @JsonProperty("hasLift") hasLift("hasLift"),
    @JsonProperty("hasNearbySprinkler") hasNearbySprinkler("hasNearbySprinkler"),
    @JsonProperty("hasSmokeDetector") hasSmokeDetector("hasSmokeDetector"),
    @JsonProperty("hasStepFreeAccess") hasStepFreeAccess("hasStepFreeAccess"),
    @JsonProperty("hasStepFreeAccessToCommunalAreas") hasStepFreeAccessToCommunalAreas("hasStepFreeAccessToCommunalAreas"),
    @JsonProperty("hasTactileFlooring") hasTactileFlooring("hasTactileFlooring"),
    @JsonProperty("hasTurningSpace") hasTurningSpace("hasTurningSpace"),
    @JsonProperty("hasWheelChairAccessibleBathrooms") hasWheelChairAccessibleBathrooms("hasWheelChairAccessibleBathrooms"),
    @JsonProperty("hasWideAccessToCommunalAreas") hasWideAccessToCommunalAreas("hasWideAccessToCommunalAreas"),
    @JsonProperty("hasWideDoor") hasWideDoor("hasWideDoor"),
    @JsonProperty("hasWideStepFreeAccess") hasWideStepFreeAccess("hasWideStepFreeAccess"),
    @JsonProperty("isArsonDesignated") isArsonDesignated("isArsonDesignated"),
    @JsonProperty("isArsonSuitable") isArsonSuitable("isArsonSuitable"),
    @JsonProperty("isCatered") isCatered("isCatered"),
    @JsonProperty("isFullyFm") isFullyFm("isFullyFm"),
    @JsonProperty("isGroundFloor") isGroundFloor("isGroundFloor"),
    @JsonProperty("isGroundFloorNrOffice") isGroundFloorNrOffice("isGroundFloorNrOffice"),
    @JsonProperty("isIAP") isIAP("isIAP"),
    @JsonProperty("isSingle") isSingle("isSingle"),
    @JsonProperty("isStepFreeDesignated") isStepFreeDesignated("isStepFreeDesignated"),
    @JsonProperty("isSuitableForVulnerable") isSuitableForVulnerable("isSuitableForVulnerable"),
    @JsonProperty("isSuitedForSexOffenders") isSuitedForSexOffenders("isSuitedForSexOffenders"),
    @JsonProperty("isTopFloorVulnerable") isTopFloorVulnerable("isTopFloorVulnerable"),
    @JsonProperty("isWheelchairAccessible") isWheelchairAccessible("isWheelchairAccessible"),
    @JsonProperty("isWheelchairDesignated") isWheelchairDesignated("isWheelchairDesignated")
}

