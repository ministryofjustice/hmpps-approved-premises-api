package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param apCode
 * @param legacyApCode The 'Q code' used in Delius to identify an Approved Premises
 * @param localAuthorityAreaName
 */
data class Premises(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "NEHOPE1", required = true, description = "")
  @get:JsonProperty("apCode", required = true) val apCode: kotlin.String,

  @Schema(example = "Q057", required = true, description = "The 'Q code' used in Delius to identify an Approved Premises")
  @get:JsonProperty("legacyApCode", required = true) val legacyApCode: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("localAuthorityAreaName", required = true) val localAuthorityAreaName: kotlin.String,
)
