package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param updatedAt
 * @param revisionType
 * @param updatedBy
 * @param startDate
 * @param endDate
 * @param reason
 * @param referenceNumber
 * @param notes
 */
data class Cas1OutOfServiceBedRevision(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("updatedAt", required = true) val updatedAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("revisionType", required = true) val revisionType: kotlin.collections.List<Cas1OutOfServiceBedRevisionType>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("updatedBy") val updatedBy: User? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("startDate") val startDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("endDate") val endDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("reason") val reason: Cas1OutOfServiceBedReason? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("referenceNumber") val referenceNumber: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
