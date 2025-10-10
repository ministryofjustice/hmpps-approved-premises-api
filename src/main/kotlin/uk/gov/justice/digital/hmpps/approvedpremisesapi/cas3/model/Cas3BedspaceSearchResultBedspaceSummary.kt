package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.util.UUID

data class Cas3BedspaceSearchResultBedspaceSummary(
  val id: UUID,
  val reference: String,
  val characteristics: List<Cas3CharacteristicPair>,
)
