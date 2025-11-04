package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Withdrawable(

  val id: java.util.UUID,

  val type: WithdrawableType,

  @Schema(example = "null", required = true, description = "0, 1 or more dates can be specified depending upon the WithdrawableType")
  val dates: kotlin.collections.List<DatePeriod>,
)
