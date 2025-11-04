package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param bookingId
 * @param previousDepartureDate
 * @param newDepartureDate
 * @param createdAt
 * @param notes
 */
data class Cas3Extension(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("previousDepartureDate", required = true) val previousDepartureDate: LocalDate,

  @get:JsonProperty("newDepartureDate", required = true) val newDepartureDate: LocalDate,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("notes") val notes: String? = null,
)
