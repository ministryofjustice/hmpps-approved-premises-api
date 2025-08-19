package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.util.UUID

data class Cas3BedspacesReference(
  val affectedBedspaces: List<Cas3BedspaceReference> = emptyList(),
)

data class Cas3BedspaceReference(
  val id: UUID,
  val reference: String,
)
