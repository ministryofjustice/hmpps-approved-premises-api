package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas1ClarificationNote(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("createdByStaffMemberId", required = true) val createdByStaffMemberId: UUID,

  @get:JsonProperty("query", required = true) val query: String,

  @get:JsonProperty("responseReceivedOn") val responseReceivedOn: LocalDate? = null,

  @get:JsonProperty("response") val response: String? = null,
)
