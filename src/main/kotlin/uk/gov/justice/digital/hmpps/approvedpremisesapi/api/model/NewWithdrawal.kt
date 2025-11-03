package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param reason
 * @param otherReason
 */
data class NewWithdrawal(

  @get:JsonProperty("reason", required = true) val reason: WithdrawalReason,

  @get:JsonProperty("otherReason") val otherReason: kotlin.String? = null,
)
