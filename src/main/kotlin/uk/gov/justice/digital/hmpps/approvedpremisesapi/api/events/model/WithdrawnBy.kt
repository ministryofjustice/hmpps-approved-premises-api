package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param staffMember
 * @param probationArea
 */
data class WithdrawnBy(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("staffMember", required = true) val staffMember: StaffMember,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("probationArea", required = true) val probationArea: ProbationArea,
)
