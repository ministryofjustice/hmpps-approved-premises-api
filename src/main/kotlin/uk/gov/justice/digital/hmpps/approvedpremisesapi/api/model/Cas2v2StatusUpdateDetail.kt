package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id
 * @param name
 * @param label
 */
data class Cas2v2StatusUpdateDetail(

  val id: UUID,

  @Schema(example = "moreInfoRequested", required = true, description = "")
  val name: String,

  @Schema(example = "More information requested", required = true, description = "")
  val label: String,
)
