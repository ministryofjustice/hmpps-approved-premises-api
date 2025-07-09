package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param person 
 * @param type 
 * @param createdAt 
 * @param expectedArrivalDate 
 * @param placementRequestId 
 * @param tier 
 * @param actualArrivalDate 
 */
data class Cas1ChangeRequestSummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: PersonSummary,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: Cas1ChangeRequestType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expectedArrivalDate", required = true) val expectedArrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("placementRequestId", required = true) val placementRequestId: java.util.UUID,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tier") val tier: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("actualArrivalDate") val actualArrivalDate: java.time.LocalDate? = null
    ) {

}

