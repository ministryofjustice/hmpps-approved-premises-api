package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param alertId 
 * @param dateCreated 
 * @param expired 
 * @param active 
 * @param comment 
 * @param dateExpires 
 */
data class PersonAcctAlert(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("alertId", required = true) val alertId: kotlin.Long,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("dateCreated", required = true) val dateCreated: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expired", required = true) val expired: kotlin.Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("active", required = true) val active: kotlin.Boolean,

    @Schema(example = "null", description = "")
    @get:JsonProperty("comment") val comment: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dateExpires") val dateExpires: java.time.LocalDate? = null
) {

}

