package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1ChangeRequestSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("person", required = true) val person: PersonSummary,

  @get:JsonProperty("type", required = true) val type: Cas1ChangeRequestType,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("expectedArrivalDate", required = true) val expectedArrivalDate: java.time.LocalDate,

  @get:JsonProperty("placementRequestId", required = true) val placementRequestId: java.util.UUID,

  @get:JsonProperty("tier") val tier: kotlin.String? = null,

  @get:JsonProperty("actualArrivalDate") val actualArrivalDate: java.time.LocalDate? = null,
)
