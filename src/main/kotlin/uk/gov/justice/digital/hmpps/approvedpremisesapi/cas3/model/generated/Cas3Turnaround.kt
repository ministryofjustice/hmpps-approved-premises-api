package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

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

  val id: UUID,

  val bookingId: UUID,

  val workingDays: Int,

  val createdAt: Instant,
)
