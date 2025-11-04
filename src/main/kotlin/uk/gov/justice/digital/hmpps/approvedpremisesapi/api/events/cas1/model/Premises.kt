package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

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

  val id: java.util.UUID,

  @Schema(example = "Hope House", required = true, description = "")
  val name: kotlin.String,

  @Schema(example = "NEHOPE1", required = true, description = "")
  val apCode: kotlin.String,

  @Schema(example = "Q057", required = true, description = "The 'Q code' used in Delius to identify an Approved Premises")
  val legacyApCode: kotlin.String,

  val localAuthorityAreaName: kotlin.String,
)
