package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param staffMember
 * @param probationArea
 * @param cru
 */
data class ApplicationAssessedAssessedBy(

  @Schema(example = "null", description = "")
  @get:JsonProperty("staffMember") val staffMember: StaffMember? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("probationArea") val probationArea: ProbationArea? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("cru") val cru: Cru? = null,
)
