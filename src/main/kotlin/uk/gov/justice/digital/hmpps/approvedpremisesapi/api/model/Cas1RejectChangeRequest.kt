package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1RejectChangeRequest(

  @get:JsonProperty("rejectionReasonId", required = true) val rejectionReasonId: java.util.UUID,

  @get:JsonProperty("decisionJson", required = true) val decisionJson: kotlin.collections.Map<kotlin.String, kotlin.Any>,
)
