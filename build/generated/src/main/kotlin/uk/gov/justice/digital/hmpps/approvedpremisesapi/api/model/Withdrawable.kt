package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param type 
 * @param dates 0, 1 or more dates can be specified depending upon the WithdrawableType
 */
data class Withdrawable(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: WithdrawableType,

    @Schema(example = "null", required = true, description = "0, 1 or more dates can be specified depending upon the WithdrawableType")
    @get:JsonProperty("dates", required = true) val dates: kotlin.collections.List<DatePeriod>
) {

}

