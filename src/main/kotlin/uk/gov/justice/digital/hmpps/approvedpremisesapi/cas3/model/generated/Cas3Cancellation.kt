package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

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

  val bookingId: UUID,

  val date: LocalDate,

  val reason: CancellationReason,

  val createdAt: Instant,

  val premisesName: String,

  val id: UUID? = null,

  val notes: String? = null,

  val otherReason: String? = null,
)
