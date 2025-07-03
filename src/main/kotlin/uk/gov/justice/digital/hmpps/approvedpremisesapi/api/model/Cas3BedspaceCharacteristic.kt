package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id
 * @param description
 * @param name
 */
data class Cas3BedspaceCharacteristic(

    @Schema(example = "952790c0-21d7-4fd6-a7e1-9018f08d8bb0", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "Is this premises catered (rather than self-catered)?", required = true, description = "")
    @get:JsonProperty("description", required = true) val description: String,

    @Schema(example = "isCatered", description = "")
    @get:JsonProperty("name") val name: String? = null
)

