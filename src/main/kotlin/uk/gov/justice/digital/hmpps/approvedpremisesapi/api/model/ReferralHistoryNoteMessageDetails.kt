package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param rejectionReason
 * @param rejectionReasonDetails
 * @param isWithdrawn
 * @param domainEvent Any object
 */
data class ReferralHistoryNoteMessageDetails(

  val rejectionReason: kotlin.String? = null,

  val rejectionReasonDetails: kotlin.String? = null,

  val isWithdrawn: kotlin.Boolean? = null,

  val domainEvent: kotlin.Any? = null,
)
