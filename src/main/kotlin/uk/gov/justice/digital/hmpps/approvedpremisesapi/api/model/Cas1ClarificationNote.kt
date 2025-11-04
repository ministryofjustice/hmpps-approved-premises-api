package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1ClarificationNote(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("createdByStaffMemberId", required = true) val createdByStaffMemberId: java.util.UUID,

  @get:JsonProperty("query", required = true) val query: kotlin.String,

  @get:JsonProperty("responseReceivedOn") val responseReceivedOn: java.time.LocalDate? = null,

  @get:JsonProperty("response") val response: kotlin.String? = null,
)
