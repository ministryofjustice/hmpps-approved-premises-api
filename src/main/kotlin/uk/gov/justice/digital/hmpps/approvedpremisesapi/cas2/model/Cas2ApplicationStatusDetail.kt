package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id
 * @param name
 * @param label
 */
data class Cas2ApplicationStatusDetail(

  val id: UUID,

  @Schema(example = "changeOfCircumstances", required = true, description = "")
  val name: String,

  @Schema(example = "Change of Circumstances", required = true, description = "")
  val label: String,
)
