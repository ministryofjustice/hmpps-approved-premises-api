package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id
 * @param name
 * @param label
 */
data class Cas2StatusUpdateDetail(

  val id: UUID,

  @Schema(example = "moreInfoRequested", required = true, description = "")
  val name: String,

  @Schema(example = "More information requested", required = true, description = "")
  val label: String,
)
