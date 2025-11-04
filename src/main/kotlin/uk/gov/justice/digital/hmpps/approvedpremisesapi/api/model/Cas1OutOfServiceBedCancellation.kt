package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1OutOfServiceBedCancellation(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
