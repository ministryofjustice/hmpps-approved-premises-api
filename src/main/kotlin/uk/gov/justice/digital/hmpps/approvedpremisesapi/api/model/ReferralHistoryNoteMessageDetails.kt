package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param rejectionReason
 * @param rejectionReasonDetails
 * @param isWithdrawn
 * @param domainEvent Any object
 */
data class ReferralHistoryNoteMessageDetails(

  @get:JsonProperty("rejectionReason") val rejectionReason: kotlin.String? = null,

  @get:JsonProperty("rejectionReasonDetails") val rejectionReasonDetails: kotlin.String? = null,

  @get:JsonProperty("isWithdrawn") val isWithdrawn: kotlin.Boolean? = null,

  @get:JsonProperty("domainEvent") val domainEvent: kotlin.Any? = null,
)
