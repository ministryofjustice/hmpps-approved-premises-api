package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import java.util.UUID

/**
 *
 * @param name
 * @param crn
 * @param personType
 * @param days
 * @param bookingId
 * @param roomId
 * @param isSexualRisk
 * @param sex
 * @param assessmentId
 */
data class Cas3BedspaceSearchResultOverlap(

  val name: String,

  val crn: String,

  val personType: PersonType,

  val days: Int,

  val bookingId: UUID,

  val roomId: UUID,

  val isSexualRisk: Boolean,

  val sex: String? = null,

  val assessmentId: UUID? = null,
)
