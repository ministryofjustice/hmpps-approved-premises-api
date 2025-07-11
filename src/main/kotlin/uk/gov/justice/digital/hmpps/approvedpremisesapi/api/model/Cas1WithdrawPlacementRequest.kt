package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason

/**
 *
 * @param reason
 */
data class Cas1WithdrawPlacementRequest(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: WithdrawPlacementRequestReason,
)
