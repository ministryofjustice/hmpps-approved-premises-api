package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param startDate
 * @param endDate
 * @param reason
 * @param bedId
 * @param referenceNumber
 * @param notes
 */
data class NewCas1OutOfServiceBed(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bedId", required = true) val bedId: java.util.UUID,

  @Schema(example = "null", description = "")
  @get:JsonProperty("referenceNumber") val referenceNumber: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
