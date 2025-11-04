package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
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

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("dateTime", required = true) val dateTime: Instant,

  @get:JsonProperty("reason", required = true) val reason: DepartureReason,

  @get:JsonProperty("moveOnCategory", required = true) val moveOnCategory: MoveOnCategory,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("notes") val notes: String? = null,
)
