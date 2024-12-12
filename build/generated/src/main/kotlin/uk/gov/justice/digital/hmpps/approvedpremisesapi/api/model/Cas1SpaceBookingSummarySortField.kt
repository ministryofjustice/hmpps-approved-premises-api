package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: personName,canonicalArrivalDate,canonicalDepartureDate,keyWorkerName,tier
*/
enum class Cas1SpaceBookingSummarySortField(val value: kotlin.String) {

    @JsonProperty("personName") personName("personName"),
    @JsonProperty("canonicalArrivalDate") canonicalArrivalDate("canonicalArrivalDate"),
    @JsonProperty("canonicalDepartureDate") canonicalDepartureDate("canonicalDepartureDate"),
    @JsonProperty("keyWorkerName") keyWorkerName("keyWorkerName"),
    @JsonProperty("tier") tier("tier")
}

