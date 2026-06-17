package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas1NewDeparture(

  @get:JsonProperty("reasonId", required = true) val reasonId: UUID,

  @Schema(example = "null", description = "use seperate date/time fields")
  @Deprecated(message = "")
  @get:JsonProperty("departureDateTime") val departureDateTime: Instant? = null,

  @get:JsonProperty("departureDate") val departureDate: LocalDate? = null,

  @Schema(example = "23:15", description = "")
  @get:JsonProperty("departureTime") val departureTime: String? = null,

  @get:JsonProperty("moveOnCategoryId") val moveOnCategoryId: UUID? = null,

  @get:JsonProperty("notes") val notes: String? = null,
)
