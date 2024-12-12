package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: created,updatedStartDate,updatedEndDate,updatedReferenceNumber,updatedReason,updatedNotes
*/
enum class Cas1OutOfServiceBedRevisionType(val value: kotlin.String) {

    @JsonProperty("created") created("created"),
    @JsonProperty("updatedStartDate") updatedStartDate("updatedStartDate"),
    @JsonProperty("updatedEndDate") updatedEndDate("updatedEndDate"),
    @JsonProperty("updatedReferenceNumber") updatedReferenceNumber("updatedReferenceNumber"),
    @JsonProperty("updatedReason") updatedReason("updatedReason"),
    @JsonProperty("updatedNotes") updatedNotes("updatedNotes")
}

