package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id
 * @param reference
 * @param addressLine1
 * @param postcode
 * @param pdu
 * @param addressLine2
 * @param town
 * @param localAuthorityAreaName
 * @param bedspaces
 * @param totalArchivedBedspaces
 */
data class Cas3PremisesSearchResult(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("reference", required = true) val reference: String,

  @Schema(example = "one something street", required = true, description = "")
  @get:JsonProperty("addressLine1", required = true) val addressLine1: String,

  @Schema(example = "LS1 3AD", required = true, description = "")
  @get:JsonProperty("postcode", required = true) val postcode: String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("pdu", required = true) val pdu: String,

  @Schema(example = "Blackmore End", description = "")
  @get:JsonProperty("addressLine2") val addressLine2: String? = null,

  @Schema(example = "Leeds", description = "")
  @get:JsonProperty("town") val town: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("localAuthorityAreaName") val localAuthorityAreaName: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("bedspaces") val bedspaces: List<Cas3BedspacePremisesSearchResult>? = null,

  @Schema(example = "4", description = "")
  @get:JsonProperty("totalArchivedBedspaces") val totalArchivedBedspaces: Int? = null,
)
