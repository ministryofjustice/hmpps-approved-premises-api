package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember

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
