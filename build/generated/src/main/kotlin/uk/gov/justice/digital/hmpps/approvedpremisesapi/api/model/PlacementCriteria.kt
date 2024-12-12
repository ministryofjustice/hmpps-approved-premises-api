package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: isPIPE,isESAP,isMHAPStJosephs,isMHAPElliottHouse,isSemiSpecialistMentalHealth,isRecoveryFocussed,hasBrailleSignage,hasTactileFlooring,hasHearingLoop,isStepFreeDesignated,isArsonDesignated,isWheelchairDesignated,isSingle,isCatered,isSuitedForSexOffenders,isSuitableForVulnerable,acceptsSexOffenders,acceptsHateCrimeOffenders,acceptsChildSexOffenders,acceptsNonSexualChildOffenders,isArsonSuitable,isGroundFloor,hasEnSuite
*/
enum class PlacementCriteria(val value: kotlin.String) {

    @JsonProperty("isPIPE") isPIPE("isPIPE"),
    @JsonProperty("isESAP") isESAP("isESAP"),
    @JsonProperty("isMHAPStJosephs") isMHAPStJosephs("isMHAPStJosephs"),
    @JsonProperty("isMHAPElliottHouse") isMHAPElliottHouse("isMHAPElliottHouse"),
    @JsonProperty("isSemiSpecialistMentalHealth") isSemiSpecialistMentalHealth("isSemiSpecialistMentalHealth"),
    @JsonProperty("isRecoveryFocussed") isRecoveryFocussed("isRecoveryFocussed"),
    @JsonProperty("hasBrailleSignage") hasBrailleSignage("hasBrailleSignage"),
    @JsonProperty("hasTactileFlooring") hasTactileFlooring("hasTactileFlooring"),
    @JsonProperty("hasHearingLoop") hasHearingLoop("hasHearingLoop"),
    @JsonProperty("isStepFreeDesignated") isStepFreeDesignated("isStepFreeDesignated"),
    @JsonProperty("isArsonDesignated") isArsonDesignated("isArsonDesignated"),
    @JsonProperty("isWheelchairDesignated") isWheelchairDesignated("isWheelchairDesignated"),
    @JsonProperty("isSingle") isSingle("isSingle"),
    @JsonProperty("isCatered") isCatered("isCatered"),
    @JsonProperty("isSuitedForSexOffenders") isSuitedForSexOffenders("isSuitedForSexOffenders"),
    @JsonProperty("isSuitableForVulnerable") isSuitableForVulnerable("isSuitableForVulnerable"),
    @JsonProperty("acceptsSexOffenders") acceptsSexOffenders("acceptsSexOffenders"),
    @JsonProperty("acceptsHateCrimeOffenders") acceptsHateCrimeOffenders("acceptsHateCrimeOffenders"),
    @JsonProperty("acceptsChildSexOffenders") acceptsChildSexOffenders("acceptsChildSexOffenders"),
    @JsonProperty("acceptsNonSexualChildOffenders") acceptsNonSexualChildOffenders("acceptsNonSexualChildOffenders"),
    @JsonProperty("isArsonSuitable") isArsonSuitable("isArsonSuitable"),
    @JsonProperty("isGroundFloor") isGroundFloor("isGroundFloor"),
    @JsonProperty("hasEnSuite") hasEnSuite("hasEnSuite")
}

