package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class BedSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @get:JsonProperty("roomName", required = true) val roomName: kotlin.String,

  @get:JsonProperty("status", required = true) val status: BedStatus,
)
