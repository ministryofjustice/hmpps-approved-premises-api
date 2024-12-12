package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param reason 
 */
data class WithdrawPlacementApplication(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reason", required = true) val reason: WithdrawPlacementRequestReason
) {

}

