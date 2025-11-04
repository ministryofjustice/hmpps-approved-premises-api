package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param reason
 * @param otherReason
 */
data class NewWithdrawal(

  val reason: WithdrawalReason,

  val otherReason: kotlin.String? = null,
)
