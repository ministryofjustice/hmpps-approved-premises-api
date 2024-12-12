package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: licence,rotl,hdc,pss,inCommunity,notApplicable,extendedDeterminateLicence,paroleDirectedLicence,reReleasedPostRecall
*/
enum class ReleaseTypeOption(val value: kotlin.String) {

    @JsonProperty("licence") licence("licence"),
    @JsonProperty("rotl") rotl("rotl"),
    @JsonProperty("hdc") hdc("hdc"),
    @JsonProperty("pss") pss("pss"),
    @JsonProperty("in_community") inCommunity("in_community"),
    @JsonProperty("not_applicable") notApplicable("not_applicable"),
    @JsonProperty("extendedDeterminateLicence") extendedDeterminateLicence("extendedDeterminateLicence"),
    @JsonProperty("paroleDirectedLicence") paroleDirectedLicence("paroleDirectedLicence"),
    @JsonProperty("reReleasedPostRecall") reReleasedPostRecall("reReleasedPostRecall")
}

