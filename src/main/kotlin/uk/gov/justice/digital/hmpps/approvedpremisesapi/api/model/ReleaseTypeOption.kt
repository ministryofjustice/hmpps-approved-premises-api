package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: licence,rotl,hdc,pss,inCommunity,notApplicable,extendedDeterminateLicence,paroleDirectedLicence,reReleasedPostRecall
*/
enum class ReleaseTypeOption(@get:JsonValue val value: kotlin.String) {

    licence("licence"),
    rotl("rotl"),
    hdc("hdc"),
    pss("pss"),
    inCommunity("in_community"),
    notApplicable("not_applicable"),
    extendedDeterminateLicence("extendedDeterminateLicence"),
    paroleDirectedLicence("paroleDirectedLicence"),
    reReleasedPostRecall("reReleasedPostRecall");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ReleaseTypeOption {
                return values().first{it -> it.value == value}
        }
    }
}

