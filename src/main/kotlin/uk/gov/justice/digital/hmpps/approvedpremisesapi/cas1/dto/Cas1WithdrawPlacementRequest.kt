package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason

data class Cas1WithdrawPlacementRequest(

  @get:JsonProperty("reason", required = true) val reason: WithdrawPlacementRequestReason,
)
