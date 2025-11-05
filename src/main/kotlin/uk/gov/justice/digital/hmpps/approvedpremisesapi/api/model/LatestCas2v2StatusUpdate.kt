package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class LatestCas2v2StatusUpdate(

  @get:JsonProperty("statusId", required = true) val statusId: UUID,

  @field:Schema(example = "More information requested", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: String,
)
