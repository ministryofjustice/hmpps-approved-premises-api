package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param rejectionReasonId
 * @param decisionJson
 */
data class Cas1RejectChangeRequest(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("rejectionReasonId", required = true) val rejectionReasonId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("decisionJson", required = true) val decisionJson: kotlin.collections.Map<kotlin.String, kotlin.Any>,
)
