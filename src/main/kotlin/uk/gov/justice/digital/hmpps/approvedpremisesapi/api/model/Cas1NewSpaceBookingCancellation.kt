package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1NewSpaceBookingCancellation(

  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.LocalDate,

  @get:JsonProperty("reasonId", required = true) val reasonId: java.util.UUID,

  @get:JsonProperty("reasonNotes") val reasonNotes: kotlin.String? = null,
)
