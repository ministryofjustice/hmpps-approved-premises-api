package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1WithdrawPlacementRequest(

  @get:JsonProperty("reason", required = true) val reason: WithdrawPlacementRequestReason,
)
