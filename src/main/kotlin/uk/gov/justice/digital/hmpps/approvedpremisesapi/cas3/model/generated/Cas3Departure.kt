package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import java.time.Instant
import java.util.UUID

/**
 *
 * @param id
 * @param bookingId
 * @param dateTime
 * @param reason
 * @param moveOnCategory
 * @param createdAt
 * @param notes
 */
data class Cas3Departure(

  val id: UUID,

  val bookingId: UUID,

  val dateTime: Instant,

  val reason: DepartureReason,

  val moveOnCategory: MoveOnCategory,

  val createdAt: Instant,

  val notes: String? = null,
)
