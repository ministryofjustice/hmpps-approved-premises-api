package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param createdAt
 * @param startDate
 * @param endDate
 * @param bed
 * @param room
 * @param premises
 * @param apArea
 * @param reason
 * @param daysLostCount
 * @param temporality
 * @param status
 * @param revisionHistory
 * @param referenceNumber
 * @param notes
 * @param cancellation
 */
data class Cas1OutOfServiceBed(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bed", required = true) val bed: NamedId,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("room", required = true) val room: NamedId,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premises", required = true) val premises: NamedId,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("apArea", required = true) val apArea: NamedId,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: Cas1OutOfServiceBedReason,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("daysLostCount", required = true) val daysLostCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("temporality", required = true) val temporality: Temporality,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: Cas1OutOfServiceBedStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("revisionHistory", required = true) val revisionHistory: kotlin.collections.List<Cas1OutOfServiceBedRevision>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("referenceNumber") val referenceNumber: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("cancellation") val cancellation: Cas1OutOfServiceBedCancellation? = null,
)
