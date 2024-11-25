package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param staffMember
 * @param probationArea
 * @param team
 * @param ldu
 * @param region
 */
data class ApplicationSubmittedSubmittedBy(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("staffMember", required = true) val staffMember: StaffMember,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("probationArea", required = true) val probationArea: ProbationArea,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("team", required = true) val team: Team,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("ldu", required = true) val ldu: Ldu,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("region", required = true) val region: Region,
)
