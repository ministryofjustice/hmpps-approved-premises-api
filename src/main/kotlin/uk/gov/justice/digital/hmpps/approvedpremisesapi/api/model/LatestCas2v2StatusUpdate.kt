package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param statusId
 * @param label
 */
data class LatestCas2v2StatusUpdate(

  val statusId: UUID,

  @Schema(example = "More information requested", required = true, description = "")
  val label: String,
)
