package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param appealDate 
 * @param appealDetail 
 * @param decision 
 * @param decisionDetail 
 */
data class NewAppeal(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("appealDate", required = true) val appealDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("appealDetail", required = true) val appealDetail: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("decision", required = true) val decision: AppealDecision,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("decisionDetail", required = true) val decisionDetail: kotlin.String
) {

}

