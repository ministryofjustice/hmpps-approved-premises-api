package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewBedMove(

  @get:JsonProperty("bedId", required = true) val bedId: java.util.UUID,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
