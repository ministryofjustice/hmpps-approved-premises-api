package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import java.util.UUID

data class Cas3v2BedspaceSearchResultOverlap(
  val name: String,
  val crn: String,
  val personType: PersonType,
  val days: Int,
  val bookingId: UUID,
  val bedspaceId: UUID,
  val isSexualRisk: Boolean,
  val sex: String? = null,
  val assessmentId: UUID? = null,
)
