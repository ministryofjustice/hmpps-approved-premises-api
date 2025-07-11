package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A member of probation or HPT staff detail
 * @param staffCode
 * @param username
 * @param probationRegionCode
 */
data class StaffMember(

  @Schema(example = "N54A999", required = true, description = "")
  @get:JsonProperty("staffCode", required = true) val staffCode: kotlin.String,

  @Schema(example = "JohnSmithNPS", required = true, description = "")
  @get:JsonProperty("username", required = true) val username: kotlin.String,

  @Schema(example = "N53", required = true, description = "")
  @get:JsonProperty("probationRegionCode", required = true) val probationRegionCode: kotlin.String,
)
