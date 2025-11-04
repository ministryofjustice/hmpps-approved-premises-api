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

  val id: UUID,

  val bookingId: UUID,

  val date: LocalDate,

  val reason: NonArrivalReason,

  val createdAt: Instant,

  val notes: String? = null,
)
