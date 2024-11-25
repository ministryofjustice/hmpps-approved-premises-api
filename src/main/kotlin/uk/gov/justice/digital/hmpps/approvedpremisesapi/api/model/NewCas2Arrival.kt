package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param arrivalDate
 */
data class NewCas2Arrival(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("expectedDepartureDate", required = true) override val expectedDepartureDate: java.time.LocalDate,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") override val notes: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("keyWorkerStaffCode") override val keyWorkerStaffCode: kotlin.String? = null,
) : NewArrival
