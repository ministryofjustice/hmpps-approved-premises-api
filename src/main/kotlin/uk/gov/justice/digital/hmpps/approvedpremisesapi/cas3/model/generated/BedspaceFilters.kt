package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param includedCharacteristicIds
 * @param excludedCharacteristicIds
 */
data class BedspaceFilters(

    @Schema(example = "null", description = "")
    @get:JsonProperty("includedCharacteristicIds") val includedCharacteristicIds: List<UUID>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("excludedCharacteristicIds") val excludedCharacteristicIds: List<UUID>? = null
)

