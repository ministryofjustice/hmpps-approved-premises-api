package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Change requests were developed but never used", deprecated = true)
@Deprecated(message = "Change requests were developed but never used")
data class Cas1ChangeRequestSummary(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("person", required = true) val person: PersonSummary,

  @get:JsonProperty("type", required = true) val type: Cas1ChangeRequestType,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("expectedArrivalDate", required = true) val expectedArrivalDate: LocalDate,

  @get:JsonProperty("placementRequestId", required = true) val placementRequestId: UUID,

  @Schema(description = "Tier when the application was created")
  @get:JsonProperty("tier") val tier: String? = null,

  @get:JsonProperty("actualArrivalDate") val actualArrivalDate: LocalDate? = null,
)
