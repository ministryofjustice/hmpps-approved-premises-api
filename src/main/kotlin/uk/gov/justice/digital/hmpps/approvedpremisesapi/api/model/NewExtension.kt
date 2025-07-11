package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param newDepartureDate
 * @param notes
 */
data class NewExtension(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("newDepartureDate", required = true) val newDepartureDate: java.time.LocalDate,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
