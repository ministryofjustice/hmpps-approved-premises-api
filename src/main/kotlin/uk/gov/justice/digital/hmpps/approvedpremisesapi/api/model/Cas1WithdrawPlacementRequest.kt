package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param reason
 */
data class Cas1WithdrawPlacementRequest(

  val reason: WithdrawPlacementRequestReason,
)
