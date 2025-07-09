package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: isPIPE,isESAP,isMHAPStJosephs,isMHAPElliottHouse,isSemiSpecialistMentalHealth,isRecoveryFocussed,hasBrailleSignage,hasTactileFlooring,hasHearingLoop,isStepFreeDesignated,isArsonDesignated,isWheelchairDesignated,isSingle,isCatered,isSuitedForSexOffenders,isSuitableForVulnerable,acceptsSexOffenders,acceptsHateCrimeOffenders,acceptsChildSexOffenders,acceptsNonSexualChildOffenders,isArsonSuitable,isGroundFloor,hasEnSuite,arsonOffences
*/
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
    arsonOffences("arsonOffences");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): PlacementCriteria {
                return values().first{it -> it.value == value}
        }
    }
}

