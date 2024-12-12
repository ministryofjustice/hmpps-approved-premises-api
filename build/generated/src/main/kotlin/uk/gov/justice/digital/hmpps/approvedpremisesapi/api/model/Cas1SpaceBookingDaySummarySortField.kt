package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: PERSON_NAME,TIER,CANONICAL_ARRIVAL_DATE,CANONICAL_DEPARTURE_DATE,RELEASE_TYPE,SPACE_TYPE
*/
enum class Cas1SpaceBookingDaySummarySortField(val value: kotlin.String) {

    @JsonProperty("personName") PERSON_NAME("personName"),
    @JsonProperty("tier") TIER("tier"),
    @JsonProperty("canonicalArrivalDate") CANONICAL_ARRIVAL_DATE("canonicalArrivalDate"),
    @JsonProperty("canonicalDepartureDate") CANONICAL_DEPARTURE_DATE("canonicalDepartureDate"),
    @JsonProperty("releaseType") RELEASE_TYPE("releaseType"),
    @JsonProperty("spaceType") SPACE_TYPE("spaceType")
}

