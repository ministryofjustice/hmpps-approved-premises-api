package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class Cas1RejectChangeRequest(

  @get:JsonProperty("rejectionReasonId", required = true) val rejectionReasonId: UUID,

  @get:JsonProperty("decisionJson", required = true) val decisionJson: Map<String, Any>,
)
