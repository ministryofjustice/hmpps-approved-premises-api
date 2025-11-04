package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import java.time.Instant
import java.util.UUID

/**
 *
 * @param dateTime
 * @param reasonId
 * @param moveOnCategoryId
 * @param notes
 */
data class Cas3NewDeparture(

  val dateTime: Instant,

  val reasonId: UUID,

  val moveOnCategoryId: UUID,

  val notes: String? = null,
)
