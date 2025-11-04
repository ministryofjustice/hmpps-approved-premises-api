package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

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

  val id: UUID,

  val bookingId: UUID,

  val dateTime: Instant,

  val createdAt: Instant,

  val notes: String? = null,
)
