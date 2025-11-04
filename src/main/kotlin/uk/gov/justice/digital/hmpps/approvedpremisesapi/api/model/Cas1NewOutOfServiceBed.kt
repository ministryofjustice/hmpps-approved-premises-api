package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1NewOutOfServiceBed(

  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

  @get:JsonProperty("reason", required = true) val reason: java.util.UUID,

  @get:JsonProperty("bedId", required = true) val bedId: java.util.UUID,

  @get:JsonProperty("referenceNumber") val referenceNumber: kotlin.String? = null,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
