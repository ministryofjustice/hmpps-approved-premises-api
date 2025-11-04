package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

/**
 *
 * @param id
 * @param bookingId
 * @param workingDays
 * @param createdAt
 */
data class Cas3Turnaround(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("workingDays", required = true) val workingDays: Int,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,
)
