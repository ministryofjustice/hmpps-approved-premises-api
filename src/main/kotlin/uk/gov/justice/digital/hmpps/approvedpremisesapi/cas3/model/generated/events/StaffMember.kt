package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import io.swagger.v3.oas.annotations.media.Schema

/**
 * A member of probation or HPT staff detail
 * @param staffCode
 * @param username
 * @param probationRegionCode
 */
data class StaffMember(

  @Schema(example = "N54A999", required = true, description = "")
  val staffCode: kotlin.String,

  @Schema(example = "JohnSmithNPS", required = true, description = "")
  val username: kotlin.String,

  @Schema(example = "N53", required = true, description = "")
  val probationRegionCode: kotlin.String,
)
