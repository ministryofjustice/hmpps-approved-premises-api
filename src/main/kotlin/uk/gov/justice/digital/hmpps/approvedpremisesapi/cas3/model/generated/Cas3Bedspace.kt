package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic

/**
 *
 * @param id
 * @param reference
 * @param startDate Start date of the bedspace availability
 * @param status
 * @param endDate End date of the bedspace availability
 * @param notes
 * @param characteristics
 * @param bedspaceCharacteristics
 */
data class Cas3Bedspace(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reference", required = true) val reference: String,

    @Schema(
        example = "Tue Jul 30 01:00:00 BST 2024",
        required = true,
        description = "Start date of the bedspace availability"
    )
    @get:JsonProperty("startDate", required = true) val startDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: Cas3BedspaceStatus,

    @Schema(example = "Mon Dec 30 00:00:00 GMT 2024", description = "End date of the bedspace availability")
    @get:JsonProperty("endDate") val endDate: LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("characteristics") val characteristics: List<Characteristic>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bedspaceCharacteristics") val bedspaceCharacteristics: List<Cas3BedspaceCharacteristic>? = null
)

