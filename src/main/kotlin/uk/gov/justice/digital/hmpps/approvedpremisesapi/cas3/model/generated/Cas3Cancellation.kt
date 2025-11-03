package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param bookingId
 * @param date
 * @param reason
 * @param createdAt
 * @param premisesName
 * @param id
 * @param notes
 * @param otherReason
 */
data class Cas3Cancellation(

  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("date", required = true) val date: LocalDate,

  @get:JsonProperty("reason", required = true) val reason: CancellationReason,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("premisesName", required = true) val premisesName: String,

  @get:JsonProperty("id") val id: UUID? = null,

  @get:JsonProperty("notes") val notes: String? = null,

  @get:JsonProperty("otherReason") val otherReason: String? = null,
)
