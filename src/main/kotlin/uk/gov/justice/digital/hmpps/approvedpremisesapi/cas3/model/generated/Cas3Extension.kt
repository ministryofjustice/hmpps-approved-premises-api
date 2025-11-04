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

  val id: UUID,

  val bookingId: UUID,

  val previousDepartureDate: LocalDate,

  val newDepartureDate: LocalDate,

  val createdAt: Instant,

  val notes: String? = null,
)
