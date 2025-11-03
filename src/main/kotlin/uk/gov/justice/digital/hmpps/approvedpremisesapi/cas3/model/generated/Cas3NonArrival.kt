package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param bookingId
 * @param date
 * @param reason
 * @param createdAt
 * @param notes
 */
data class Cas3NonArrival(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("date", required = true) val date: LocalDate,

  @get:JsonProperty("reason", required = true) val reason: NonArrivalReason,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("notes") val notes: String? = null,
)
