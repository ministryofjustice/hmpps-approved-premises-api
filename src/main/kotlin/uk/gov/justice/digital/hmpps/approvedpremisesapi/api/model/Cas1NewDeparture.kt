package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1NewDeparture(

  @get:JsonProperty("reasonId", required = true) val reasonId: java.util.UUID,

  @Schema(example = "null", description = "use seperate date/time fields")
  @Deprecated(message = "")
  @get:JsonProperty("departureDateTime") val departureDateTime: java.time.Instant? = null,

  @get:JsonProperty("departureDate") val departureDate: java.time.LocalDate? = null,

  @Schema(example = "23:15", description = "")
  @get:JsonProperty("departureTime") val departureTime: kotlin.String? = null,

  @get:JsonProperty("moveOnCategoryId") val moveOnCategoryId: java.util.UUID? = null,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
