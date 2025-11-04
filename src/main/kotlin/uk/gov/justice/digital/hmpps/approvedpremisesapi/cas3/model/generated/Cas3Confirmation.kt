package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

/**
 *
 * @param id
 * @param bookingId
 * @param dateTime
 * @param createdAt
 * @param notes
 */
data class Cas3Confirmation(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("dateTime", required = true) val dateTime: Instant,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("notes") val notes: String? = null,
)
