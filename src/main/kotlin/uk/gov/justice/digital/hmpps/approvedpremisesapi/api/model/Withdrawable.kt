package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Withdrawable(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("type", required = true) val type: WithdrawableType,

  @Schema(example = "null", required = true, description = "0, 1 or more dates can be specified depending upon the WithdrawableType")
  @get:JsonProperty("dates", required = true) val dates: kotlin.collections.List<DatePeriod>,
)
