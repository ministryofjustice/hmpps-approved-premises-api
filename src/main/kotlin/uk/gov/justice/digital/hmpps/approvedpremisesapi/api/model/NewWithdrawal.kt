package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class NewWithdrawal(

  val reason: WithdrawalReason,

  val otherReason: kotlin.String? = null,
)
