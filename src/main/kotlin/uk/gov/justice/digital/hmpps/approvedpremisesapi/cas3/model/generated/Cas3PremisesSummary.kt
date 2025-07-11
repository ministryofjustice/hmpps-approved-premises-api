package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import java.util.UUID

/**
 *
 * @param id
 * @param name
 * @param addressLine1
 * @param postcode
 * @param pdu
 * @param bedspaceCount
 * @param status
 * @param addressLine2
 * @param localAuthorityAreaName
 */
data class Cas3PremisesSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @Schema(example = "one something street", required = true, description = "")
  @get:JsonProperty("addressLine1", required = true) val addressLine1: String,

  @Schema(example = "LS1 3AD", required = true, description = "")
  @get:JsonProperty("postcode", required = true) val postcode: String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("pdu", required = true) val pdu: String,

  @Schema(example = "22", required = true, description = "")
  @get:JsonProperty("bedspaceCount", required = true) val bedspaceCount: Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: PropertyStatus,

  @Schema(example = "Blackmore End", description = "")
  @get:JsonProperty("addressLine2") val addressLine2: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("localAuthorityAreaName") val localAuthorityAreaName: String? = null,
)
