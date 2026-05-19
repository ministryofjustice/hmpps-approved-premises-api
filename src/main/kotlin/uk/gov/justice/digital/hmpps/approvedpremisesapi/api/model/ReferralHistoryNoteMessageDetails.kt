package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ReferralHistoryNoteMessageDetails(

  @get:JsonProperty("rejectionReason") val rejectionReason: String? = null,

  @get:JsonProperty("rejectionReasonDetails") val rejectionReasonDetails: String? = null,

  @get:JsonProperty("isWithdrawn") val isWithdrawn: Boolean? = null,

  @get:JsonProperty("domainEvent") val domainEvent: Any? = null,
)
