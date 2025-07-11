package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param reason
 * @param otherReason
 */
data class NewWithdrawal(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: WithdrawalReason,

  @Schema(example = "null", description = "")
  @get:JsonProperty("otherReason") val otherReason: kotlin.String? = null,
)
