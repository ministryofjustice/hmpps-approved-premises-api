package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/**
 *
 * @param dateTime
 * @param reasonId
 * @param moveOnCategoryId
 * @param notes
 */
data class Cas3NewDeparture(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("dateTime", required = true) val dateTime: Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reasonId", required = true) val reasonId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("moveOnCategoryId", required = true) val moveOnCategoryId: UUID,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: String? = null
)

