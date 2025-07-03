package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id
 * @param reference
 * @param status
 */
data class Cas3BedspacePremisesSearchResult(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reference", required = true) val reference: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: Cas3BedspaceStatus
)

