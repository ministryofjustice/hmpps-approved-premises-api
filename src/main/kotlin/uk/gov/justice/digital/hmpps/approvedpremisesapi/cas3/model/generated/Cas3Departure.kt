package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import java.time.Instant
import java.util.UUID

/**
 *
 * @param id
 * @param bookingId
 * @param dateTime
 * @param reason
 * @param moveOnCategory
 * @param createdAt
 * @param notes
 */
data class Cas3Departure(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("dateTime", required = true) val dateTime: Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: DepartureReason,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("moveOnCategory", required = true) val moveOnCategory: MoveOnCategory,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: String? = null,
)
